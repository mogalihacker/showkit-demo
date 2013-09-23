package com.showkith264;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.jcodec.codecs.h264.io.model.NALUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class MainActivity extends Activity {

	private Camera myCamera;
	private MyCameraSurfaceView myCameraSurfaceView;
	private MediaRecorder mediaRecorder;
	Button myButton, myPreview;
	SurfaceHolder surfaceHolder;
	boolean recording;
	private GLSurfaceView mGLView;
	FrameLayout myCameraPreview;

	public native int stringFromJNICPP(byte[] message, int size);

	static {
		System.loadLibrary("GLSurfaceView");
	}
	InputStream is;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		recording = false;

		setContentView(R.layout.main);

		// Get Camera for preview
		myCamera = getCameraInstance();
		if (myCamera == null) {
			Toast.makeText(MainActivity.this, "Fail to get Camera",
					Toast.LENGTH_LONG).show();
		}

		myCameraSurfaceView = new MyCameraSurfaceView(this, myCamera);
		myCameraPreview = (FrameLayout) findViewById(R.id.videoview);
		myCameraPreview.addView(myCameraSurfaceView);
		mGLView = new ClearGLSurfaceView(this);
		myButton = (Button) findViewById(R.id.mybutton);
		myPreview = (Button) findViewById(R.id.mypreview);
		myButton.setOnClickListener(myButtonOnClickListener);
		mGLView.setBackgroundColor(Color.RED);
		myPreview.setOnClickListener(new OnClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onClick(View v) {
				mGLView = new ClearGLSurfaceView(MainActivity.this);

				// mGLView.draw(canvas);
				// setContentView(mGLView);
				GLSurfaceView glSurfaceView = new ClearGLSurfaceView(
						MainActivity.this);
				setContentView(glSurfaceView, new LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				View videoView = View.inflate(MainActivity.this,
						R.layout.dialogvideo, null);
				addContentView(videoView, new LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				VideoView mVideoView = (VideoView) videoView
						.findViewById(R.id.videoView1);
				videoView.setVisibility(View.VISIBLE);
				mVideoView.setVisibility(View.VISIBLE);

				mVideoView.setVideoPath("/storage/sdcard0/myvideo.mp4");
				// mVideoView.setVideoURI(Uri.parse("android.resource://com.example.glsurfaceview/"
				// + R.raw.aa));

				mVideoView.setZOrderOnTop(true);
				int i = mVideoView.getDuration();
				i = i * 30;
				mVideoView.setMediaController(new MediaController(
						MainActivity.this));
				mVideoView.requestFocus();
				mVideoView.start();
				try {

					File file = new File(Environment
							.getExternalStorageDirectory(), "myvideo.mp4");
					byte[] data = new byte[(int) file.length()];
					is = new FileInputStream(file);
					is.read(data);
					int length = (int) file.length();
					// Toast.makeText(this.getApplicationContext(),""+file.length(),
					// Toast.LENGTH_LONG).show();
					// stringFromJNICPP(data, length);

					ByteBuffer buffer = ByteBuffer.wrap(data);
					NALUnit nalunit = NALUnit.read(buffer);
					System.out.println("nal_ref_idc : " + nalunit.nal_ref_idc);
					System.out.println("nalunit : "
							+ nalunit.type.NON_IDR_SLICE.getName().toString());

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// String test = "ABC";
				// byte[] message = test.getBytes();
				// int size = message.length;
				// // public static native void SendMessage( message,size);
				// //size
				// // = message

				// String inputname = "ssssws";
				// Toast.makeText(MainActivity.this,
				// "" + stringFromJNICPP(message, size), Toast.LENGTH_LONG)
				// .show();

			}

		});

	}

	Button.OnClickListener myButtonOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (recording) {
				// stop recording and release camera
				mediaRecorder.stop(); // stop the recording
				releaseMediaRecorder(); // release the MediaRecorder object
				myButton.setText("Rec");
				recording = false;
				// Exit after saved

			} else {

				// Release Camera before MediaRecorder start
				releaseCamera();

				if (!prepareMediaRecorder()) {
					Toast.makeText(MainActivity.this,
							"Fail in prepareMediaRecorder()!\n - Ended -",
							Toast.LENGTH_LONG).show();
					finish();
				}

				mediaRecorder.start();
				recording = true;
				myButton.setText("STOP");
			}
		}
	};

	private Camera getCameraInstance() {
		// TODO Auto-generated method stub
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	@SuppressLint("NewApi")
	/*
	 * recording
	 */
	private boolean prepareMediaRecorder() {
		myCamera = getCameraInstance();

		Parameters parameters = myCamera.getParameters();

		myCamera.setParameters(parameters);

		mediaRecorder = new MediaRecorder();

		myCamera.unlock();
		mediaRecorder.setCamera(myCamera);

		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

		mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		// mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
		mediaRecorder.setVideoFrameRate(30);
		mediaRecorder.setOutputFile("/sdcard/myvideo.mp4");

		mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder()
				.getSurface());

		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder(); // if you are using MediaRecorder, release it
								// first
		releaseCamera(); // release the camera immediately on pause event
	}

	@SuppressLint("NewApi")
	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			mediaRecorder.reset(); // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			myCamera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		if (myCamera != null) {
			myCamera.release(); // release the camera for other applications
			myCamera = null;
		}
	}

	public class MyCameraSurfaceView extends SurfaceView implements
			SurfaceHolder.Callback {

		private SurfaceHolder mHolder;
		private Camera mCamera;

		public MyCameraSurfaceView(Context context, Camera camera) {
			super(context);
			mCamera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format,
				int weight, int height) {
			// If your preview can change or rotate, take care of those events
			// here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null) {
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}

			// make any resize, rotate or reformatting changes here

			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();

			} catch (Exception e) {
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}
	}
}

class ClearGLSurfaceView extends GLSurfaceView {
	public ClearGLSurfaceView(Context context) {
		super(context);
		mRenderer = new ClearRenderer();
		setRenderer(mRenderer);
	}

	public boolean onTouchEvent(final MotionEvent event) {
		queueEvent(new Runnable() {
			public void run() {

			}
		});
		return true;
	}

	ClearRenderer mRenderer;
}

class ClearRenderer implements GLSurfaceView.Renderer {
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Do nothing special.
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		gl.glViewport(0, 0, w, h);
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	public void setColor(float r, float g, float b) {
		mRed = r;
		mGreen = g;
		mBlue = b;
	}

	private float mRed;
	private float mGreen;
	private float mBlue;
}