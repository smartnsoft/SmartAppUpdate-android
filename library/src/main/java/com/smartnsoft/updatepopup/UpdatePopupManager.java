package com.smartnsoft.updatepopup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AnyThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.smartnsoft.updatepopup.bo.UpdatePopupInformations;

/**
 * @author Adrien Vitti
 * @since 2018.01.23
 */
@SuppressWarnings("unused")
public final class UpdatePopupManager
    implements OnCompleteListener<Void>
{

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({ INFORMATIVE_UPDATE, RECOMMENDED_UPDATE, BLOCKING_UPDATE })
  public @interface UpdatePopupType {}

  private static boolean isUpdateTypeKnown(long updateType)
  {
    return updateType == INFORMATIVE_UPDATE
        || updateType == RECOMMENDED_UPDATE
        || updateType == BLOCKING_UPDATE;
  }

  public static class Builder
  {

    final UpdatePopupManager updatePopupManager;

    public Builder(@NonNull Context context, boolean isInDevelopmentMode)
    {
      this.updatePopupManager = new UpdatePopupManager(context, isInDevelopmentMode);
    }

    public Builder setUpdatePopupActivity(Class<? extends UpdatePopupActivity> updatePopupActivity)
    {
      updatePopupManager.setUpdatePopupActivityClass(updatePopupActivity);
      return this;
    }

    public Builder setUpdatePopupActivity(long synchronousTimeoutInMilliseconds)
    {
      if (synchronousTimeoutInMilliseconds <= 0)
      {
        throw new IllegalArgumentException("Timeout cannot be lower or equal to 0");
      }
      updatePopupManager.setSynchronousTimeoutInMillisecond(synchronousTimeoutInMilliseconds);
      return this;
    }

    public UpdatePopupManager build()
    {
      return this.updatePopupManager;
    }
  }

  static final int INFORMATIVE_UPDATE = 1;

  static final int RECOMMENDED_UPDATE = 2;

  static final int BLOCKING_UPDATE = 3;

  private static final long SYNCHRONISATION_TIMEOUT_IN_MILLISECONDS = 60 * 1000;

  private static final String TAG = "UpdatePopupManager";

  private static final String REMOTE_CONFIG_TITLE = "title";

  private static final String REMOTE_CONFIG_IMAGE_URL = "image";

  private static final String REMOTE_CONFIG_CONTENT = "content";

  private static final String REMOTE_CONFIG_BUTTON_TEXT = "actionButton";

  private static final String REMOTE_CONFIG_UPDATE_NEEDED = "update_needed";

  private static final String REMOTE_CONFIG_IS_BLOCKING_UPDATE = "is_blocking_update";

  private static final String REMOTE_CONFIG_ACTION_URL = "deeplink";

  private static final String REMOTE_CONFIG_PACKAGE_NAME_FOR_UPDATE = "package_name_for_update";

  private static final String REMOTE_CONFIG_DIALOG_TYPE = "dialogType";

  static final String UPDATE_INFORMATION_EXTRA = "updateInformationExtra";

  private final FirebaseRemoteConfig firebaseRemoteConfig;

  private final boolean isInDevelopmentMode;

  private final Context applicationContext;

  private Class<? extends UpdatePopupActivity> updatePopupActivityClass = UpdatePopupActivity.class;

  private long synchronousTimeoutInMillisecond = UpdatePopupManager.SYNCHRONISATION_TIMEOUT_IN_MILLISECONDS;

  private UpdatePopupManager(@NonNull Context context, final boolean isInDevelopmentMode)
  {
    this.applicationContext = context.getApplicationContext();
    firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    this.isInDevelopmentMode = isInDevelopmentMode;
    FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
        .setDeveloperModeEnabled(isInDevelopmentMode)
        .build();
    firebaseRemoteConfig.setConfigSettings(configSettings);
  }

  void setUpdatePopupActivityClass(
      Class<? extends UpdatePopupActivity> updatePopupActivityClass)
  {
    this.updatePopupActivityClass = updatePopupActivityClass;
  }

  void setSynchronousTimeoutInMillisecond(long synchronousTimeoutInMillisecond)
  {
    this.synchronousTimeoutInMillisecond = synchronousTimeoutInMillisecond;
  }

  @AnyThread
  public void fetchRemoteConfig()
  {
    if (firebaseRemoteConfig != null)
    {
      long cacheExpiration = 3600; // 1 hour in seconds.
      // If your app is using developer mode, cacheExpiration is set to 0, so each fetch will
      // retrieve values from the service.
      if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled())
      {
        cacheExpiration = 0;
      }
      firebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener(this);
    }
  }

  @Override
  public void onComplete(@NonNull Task<Void> task)
  {
    if (task.isSuccessful())
    {
      onUpdateSucessful();
    }
  }

  private void onUpdateSucessful()
  {
    firebaseRemoteConfig.activateFetched();
    /*
     * Add Logic for the remote config :
     * is update needed
     * is update blocking
     * is information about features
     */

    final UpdatePopupInformations updatePopupInformations = new UpdatePopupInformations(firebaseRemoteConfig.getString(UpdatePopupManager.REMOTE_CONFIG_TITLE),
        firebaseRemoteConfig.getString(UpdatePopupManager.REMOTE_CONFIG_IMAGE_URL),
        firebaseRemoteConfig.getString(UpdatePopupManager.REMOTE_CONFIG_CONTENT),
        firebaseRemoteConfig.getString(UpdatePopupManager.REMOTE_CONFIG_BUTTON_TEXT),
        firebaseRemoteConfig.getString(UpdatePopupManager.REMOTE_CONFIG_ACTION_URL),
        firebaseRemoteConfig.getString(UpdatePopupManager.REMOTE_CONFIG_PACKAGE_NAME_FOR_UPDATE),
        (int) firebaseRemoteConfig.getLong(UpdatePopupManager.REMOTE_CONFIG_DIALOG_TYPE));

    final boolean isUpdateTypeKnown = isUpdateTypeKnown(updatePopupInformations.updatePopupType);
    if (isInDevelopmentMode)
    {
      Log.d(TAG, "UpdateType=" + updatePopupInformations.updatePopupType + " which can" + (isUpdateTypeKnown ? "" : "not ") + " be processed");
    }
    if (isUpdateTypeKnown)
    {
      final Intent intent = new Intent(applicationContext, updatePopupActivityClass);
      intent.putExtra(UpdatePopupManager.UPDATE_INFORMATION_EXTRA, updatePopupInformations);
      applicationContext.startActivity(intent);
    }
  }

  @WorkerThread
  public void fetchRemoteConfigSync()
  {
    if (firebaseRemoteConfig != null)
    {
      final Object mutex = new Object();
      long cacheExpiration = 3600; // 1 hour in seconds.
      // If your app is using developer mode, cacheExpiration is set to 0, so each fetch will
      // retrieve values from the service.
      if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled())
      {
        cacheExpiration = 0;
      }

      final long startTime = System.currentTimeMillis();
      firebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener(new OnCompleteListener<Void>()
      {
        @Override
        public void onComplete(@NonNull Task<Void> task)
        {
          if (isInDevelopmentMode)
          {
            Log.d(TAG, "Synchronous Remote Config retrieving task took " + (System.currentTimeMillis() - startTime) + "ms");
          }
          if (task.isSuccessful())
          {
            onUpdateSucessful();
          }
          else
          {
            if (isInDevelopmentMode)
            {
              Log.w(TAG, "Synchronous Remote Config retrieving task has failed");
            }
          }
          synchronized (mutex)
          {
            mutex.notify();
          }
        }
      });

      try
      {
        synchronized (mutex)
        {
          mutex.wait(synchronousTimeoutInMillisecond);
        }
      }
      catch (InterruptedException exception)
      {
        if (isInDevelopmentMode)
        {
          Log.w(TAG, "An interruption was caught when retrieving remote config synchronously", exception);
        }
      }
    }
  }


}
