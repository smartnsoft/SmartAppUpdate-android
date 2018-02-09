package com.smartnsoft.updatepopupsample;

import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;

import com.bumptech.glide.Glide;
import com.smartnsoft.smartappupdate.SmartAppUpdateActivity;

/**
 * @author Adrien Vitti
 * @since 2018.01.25
 */

public final class CustomUpdatePopupActivity
    extends SmartAppUpdateActivity
{

  @Override
  protected void setImage(@Nullable String imageURLFromRemoteConfig)
  {
    if (TextUtils.isEmpty(imageURLFromRemoteConfig) == false)
    {
      image.setVisibility(View.VISIBLE);
      Glide.with(this).load(imageURLFromRemoteConfig).into(image);
    }
  }

  @Override
  protected void setContent(@Nullable String contentFromRemoteConfig)
  {
    if (TextUtils.isEmpty(contentFromRemoteConfig) == false)
    {
      paragraph.setText(Html.fromHtml(contentFromRemoteConfig));
    }
  }
}
