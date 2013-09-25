package com.showkith264;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
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
	File file = null, file2 = new File(
			Environment.getExternalStorageDirectory(), "myvideo.mp4");
	String localhost = "127.0.0.1";
	final int port = 2303;

	// for client side implementation
	LocalSocket outSocket = null;
	Socket s1 = null;
	BufferedReader br = null;
	PrintWriter p = null;

	// for server side implementation.
	LocalServerSocket mLocalServerSocket = null;
	LocalSocket mLocalClientSocket = null;

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

		startServer();
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
				mVideoView.setVideoPath("/sdcard/myvideo.mp4");
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
					file = new File(Environment.getExternalStorageDirectory(),
							"myvideo.mp4");
					byte[] data = new byte[(int) file.length()];
					is = new FileInputStream(file);
					is.read(data);
					// int length = (int) file.length();
					ByteBuffer buffer = ByteBuffer.wrap(data);

					NALUnit nalunit = NALUnit.read(buffer);
					System.out.println("nal_ref_idc : " + nalunit.nal_ref_idc);
					System.out.println("nalunit  Value: "
							+ NALUnitType.NON_IDR_SLICE.getValue());
					System.out.println("nalunit Name : "
							+ NALUnitType.NON_IDR_SLICE.getName().toString());
					LayoutInflater inflater = getLayoutInflater();

					View layout = inflater
							.inflate(
									R.layout.custom_toast,
									(ViewGroup) findViewById(R.id.custom_toast_layout_id));
					ImageView image = (ImageView) layout
							.findViewById(R.id.image);
					image.setImageResource(R.drawable.ic_launcher);
					TextView text = (TextView) layout.findViewById(R.id.text);
					text.setText("nalunit  Value: "
							+ NALUnitType.NON_IDR_SLICE.getValue() + "\n"
							+ "nalunit Name : "
							+ NALUnitType.NON_IDR_SLICE.getName().toString());
					Toast toast = new Toast(getApplicationContext());
					toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					toast.setDuration(Toast.LENGTH_LONG);
					toast.setView(layout);
					toast.show();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void startServer() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					mLocalServerSocket = new LocalServerSocket(localhost);
				} catch (Exception e) {
					Log.e("!!!!!!!!!!!!!!!!", "Error creating server socket: "
							+ e);
					return;
				}
				while (true) {
					FileOutputStream fop = null;
					if (mLocalServerSocket == null) {
						break;
					}
					try {
						mLocalClientSocket = mLocalServerSocket.accept();
						InputStream in = mLocalClientSocket.getInputStream();
						// out = new File(mContext.getExternalFilesDir(null),
						// "testfile.mp4");
						fop = new FileOutputStream(file2);
						int len = 0;
						byte[] buffer = new byte[64 * 1024];
						while ((len = in.read(buffer)) >= 0) {
							Log.i("!~!!!!!!!!!", "Writing " + len + " bytes");
							fop.write(buffer, 0, len);
						}
					} catch (Exception e) {

						e.printStackTrace();
					} finally {
						try {
							fop.close();
							mLocalClientSocket.close();
							mLocalServerSocket.close();
							mLocalServerSocket = null;
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

	Button.OnClickListener myButtonOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			if (recording) {
				// stop recording and release camera
				mediaRecorder.stop(); // stop the recording
				releaseMediaRecorder(); // release the MediaRecorder object
				myButton.setText("Rec");
				recording = false;
				destroyConnections();

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

	private void destroyConnections() {
		try {
			if (mLocalServerSocket != null) {
				mLocalClientSocket.close();
				mLocalServerSocket.close();
			}
			mLocalServerSocket = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Closing the app.");
		Toast.makeText(getBaseContext(), "dfsdfsd", Toast.LENGTH_LONG).show();
	}

	private Camera getCameraInstance() {
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
		mediaRecorder.setVideoFrameRate(30);
		// mediaRecorder.setOutputFile("/sdcard/myvideo.mp4");

		outSocket = new LocalSocket();

		try {
			outSocket.connect(new LocalSocketAddress(localhost));
			outSocket.setSendBufferSize(64 * 1024);

			mediaRecorder.setOutputFile(outSocket.getFileDescriptor());
			mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder()
					.getSurface());
			mediaRecorder.prepare();

		} catch (IllegalStateException e) {
			System.out.println("exception IllegalStateException : " + e);
			releaseMediaRecorder();
			return false;

		} catch (IOException e) {
			System.out.println("IOException : " + e);
			releaseMediaRecorder();
			return false;

		} finally {
			try {
				outSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
			try {
				// if(outSocket!=null)
				outSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

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
				System.out
						.println("exception :ignore: tried to stop a non-existent preview "
								+ e);
			}
			// make any resize, rotate or reformatting changes here
			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			} catch (Exception e) {
				System.out.println("exception : 111 " + e);
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				System.out.println("surface created:before preview");
				mCamera.startPreview();
				System.out.println("surface created:after preview");

			} catch (IOException e) {
				System.out.println("IO Exception 112");
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			System.out.println("surface distroyed");

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

	}

	public void setColor(float r, float g, float b) {

	}

}