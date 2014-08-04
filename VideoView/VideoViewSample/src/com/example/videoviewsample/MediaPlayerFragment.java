package com.example.videoviewsample;

import java.io.IOException;

import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

public class MediaPlayerFragment extends Fragment implements
		TextureView.SurfaceTextureListener {

	// Log tag.
	private static final String TAG = MediaPlayerFragment.class.getName();

	// Asset video file name.
	private static final String VIDEO_URL = "http://giftsoninternet.com/android/EngageApp/VuforiaSizzleReel_1.m4v";

	private View view;
	private SurfaceTexture mTexture;
	private MediaPlayer mMediaPlayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.setRetainInstance(true);

		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.texture_view_simple, container, false);

		// Initialize surface
		TextureView textureView = (TextureView) view
				.findViewById(R.id.textureView);
		textureView.setSurfaceTextureListener(this);

		return view;
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i,
			int i2) {
		
		mTexture = surfaceTexture;
		Surface surface = new Surface(mTexture);
		
		if (mMediaPlayer == null) {
			try {
				mMediaPlayer = new MediaPlayer();
				mMediaPlayer.setDataSource(view.getContext(),
						Uri.parse(VIDEO_URL));
				// mMediaPlayer.setVolume(0, 0);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepareAsync();

				// Play video when the media source is ready for playback.
				mMediaPlayer
						.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
							@Override
							public void onPrepared(MediaPlayer mediaPlayer) {
								mediaPlayer.start();
							}
						});

			} catch (IllegalArgumentException e) {
				Log.d(TAG, e.getMessage());
			} catch (SecurityException e) {
				Log.d(TAG, e.getMessage());
			} catch (IllegalStateException e) {
				Log.d(TAG, e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, e.getMessage());
			}
		} 
		
		mMediaPlayer.setSurface(surface);
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
			int i, int i2) {
		mTexture = surfaceTexture;
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
		mTexture = surfaceTexture;

		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
		mTexture = surfaceTexture;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

}
