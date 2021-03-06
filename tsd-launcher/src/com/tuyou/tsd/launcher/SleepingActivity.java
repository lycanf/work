package com.tuyou.tsd.launcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tuyou.tsd.R;
import com.tuyou.tsd.common.CommonApps;
import com.tuyou.tsd.common.CommonMessage;
import com.tuyou.tsd.common.TSDConst;
import com.tuyou.tsd.common.TSDEvent;
import com.tuyou.tsd.common.base.BaseActivity;
import com.tuyou.tsd.common.network.GetWeatherRes;
import com.tuyou.tsd.common.util.HelperUtil;
import com.tuyou.tsd.common.util.LogUtil;
import com.tuyou.tsd.core.CoreService;
import com.tuyou.tsd.core.CoreService.WorkingMode;

public class SleepingActivity extends BaseActivity {
	private static final String TAG = "SleepingActivity";

	private View mFaceView;
	private TextView mTimeView, mDateView, mWeatherTextView, mHighTempView, mLowTempView;
	private TimeTask mTimeTask = null;
	private IntentFilter mIntentFilter;

	private CoreService mBindService;
	private int mClickdTimes;
	private Toast mToast;
	
	//music 
	private RelativeLayout mMusicLayout = null;
	private TextView mMusicName = null;
	private ImageView mMusicPre = null;
	private ImageView mMusicPlay = null;
	private ImageView mMusicNext = null;
			

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBindService = ((CoreService.LocalBinder)service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBindService = null;
		}
		
	};

	private Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) { 
			case 100:
				updateTime();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			LogUtil.d(TAG, "receive the broadcast: " + intent);
			String action = intent.getAction();
			
    		if(action.equals(CommonApps.SLEEP_SHOW_CONTENT)){
    			String musictitle = intent.getStringExtra(CommonApps.SLEEP_CONTENT_TITLE);
    			Log.v(TAG,"SLEEP_SHOW_CONTENT ="+musictitle);
    			if(musictitle==null || musictitle.equals("")){
    				setMusicLayout(false);
    			}else{
    				setMusicLayout(true);
    				mMusicName.setText(musictitle);
//    				mMusicPlay.setBackgroundResource(CoreService.SLEEP_MUSIC_IS_PLAYING ? R.drawable.music_ctl_pause : R.drawable.music_ctl_play);
    			}
    		}else if(action.equals(CommonApps.SLEEP_BEEN_PLAY_MUSIC)){
    			boolean isPlay = intent.getBooleanExtra(CommonApps.SLEEP_PLAY_MUSIC_NEED_PLAY, true);
    			mMusicPlay.setBackgroundResource(isPlay ? R.drawable.music_ctl_pause : R.drawable.music_ctl_play);
    		}else if(action.equals(CommonApps.SLEEP_BEEN_PLAY_MUSIC)){
    			boolean isPlay = intent.getBooleanExtra(CommonApps.SLEEP_PLAY_MUSIC_NEED_PLAY, true);
    			mMusicPlay.setBackgroundResource(isPlay ? R.drawable.music_ctl_pause : R.drawable.music_ctl_play);
    		}

			if (action.equals(CommonMessage.VOICE_COMM_WAKEUP)) {
				onWakeUp();
			}
			else if (action.equals(TSDEvent.System.WEATHER_UPDATED)) {
				// 设置天气数据
				GetWeatherRes data = intent.getParcelableExtra("data");
				if (data != null) {
					setWeatherData(data);
				}
			}
		}
		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.sleeping_activity);
		initView();
		bindService(new Intent(this, CoreService.class), mConnection, Context.BIND_AUTO_CREATE);
		
	}

	
	@Override
	protected void onDestroy() {
		unbindService(mConnection);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(mReceiver, mIntentFilter);

		if (mTimeTask == null) {
			mTimeTask = new TimeTask();
			mTimeTask.start();
		}
		// 请求天气信息
		sendBroadcast(new Intent(TSDEvent.System.QUERY_WEATHER));
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mReceiver);

		if (mTimeTask != null) {
			mTimeTask.stop = true;
			mTimeTask = null;
		}
	}

	private void initView() {
		mFaceView = (View) findViewById(R.id.sleeping_view);
		mFaceView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				canWakeUpFromSleep();
			}
		});
		// 根据需求定义，暂时设为不显示
		mFaceView.setVisibility(View.GONE);
		
		mMusicLayout = (RelativeLayout) findViewById(R.id.sleep_music_view);
		mMusicName = (TextView) findViewById(R.id.sleep_music_name);
		mMusicPre = (ImageView) findViewById(R.id.sleep_music_pre);
		mMusicPre.setOnClickListener(mMusicListen);
		mMusicPlay = (ImageView) findViewById(R.id.sleep_music_play);
		mMusicPlay.setOnClickListener(mMusicListen);
		mMusicNext = (ImageView) findViewById(R.id.sleep_music_next);
		mMusicNext.setOnClickListener(mMusicListen);
		mMusicLayout.setVisibility(View.INVISIBLE);
		
		// Temporary solution
		findViewById(R.id.sleeping_layout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				canWakeUpFromSleep();
			}
		});

		mTimeView = (TextView) findViewById(R.id.sleeping_time_view);
		mDateView = (TextView) findViewById(R.id.sleeping_date_text);
		mWeatherTextView = (TextView) findViewById(R.id.sleeping_weather_text);
		mHighTempView = (TextView) findViewById(R.id.sleeping_high_temp);
		mLowTempView  = (TextView) findViewById(R.id.sleeping_low_temp);

		SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日E", Locale.CHINA);
		String strDate = sdf.format(new Date(System.currentTimeMillis()));
		mDateView.setText(strDate);

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(CommonMessage.VOICE_COMM_WAKEUP);
		mIntentFilter.addAction(TSDEvent.System.WEATHER_UPDATED);
		mIntentFilter.addAction(CommonApps.SLEEP_SHOW_CONTENT);
		mIntentFilter.addAction(CommonApps.SLEEP_BEEN_PLAY_MUSIC);
		
		mIntentFilter.addAction(CommonApps.SLEEP_BEEN_PLAY_MUSIC);
	}

	private void onWakeUp() {
		canWakeUpFromSleep();
	}
	
	public void setMusicLayout(boolean bVisible){
		Log.v(TAG,"setMusicLayout "+bVisible);
		if(mMusicLayout!=null){
			mMusicLayout.setVisibility(bVisible ? View.VISIBLE : View.VISIBLE);
		}
	}

	private OnClickListener mMusicListen = new OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			Intent intent ;
			switch(arg0.getId()){
			case R.id.sleep_music_pre:
				intent= new Intent(CommonApps.SLEEP_PLAY_PRE);
				sendBroadcast(intent);
				break;
			case R.id.sleep_music_play:
				setPlayState();
				break;
			case R.id.sleep_music_next:
				intent= new Intent(CommonApps.SLEEP_PLAY_NEXT);
				sendBroadcast(intent);
				break;
			}
		}
	};
	
	boolean mbIsMusicPlay = true;
	private void setPlayState(){
		mbIsMusicPlay = !mbIsMusicPlay;
		Intent intent = new Intent(CommonApps.SLEEP_PLAY_MUSIC);
		if(mbIsMusicPlay){
			intent.putExtra(CommonApps.SLEEP_PLAY_MUSIC_NEED_PLAY, true);
		}else{
			intent.putExtra(CommonApps.SLEEP_PLAY_MUSIC_NEED_PLAY, false);
		}
		mMusicPlay.setBackgroundResource(mbIsMusicPlay ? R.drawable.music_ctl_pause : R.drawable.music_ctl_play);
		sendBroadcast(intent);
	}
	
	private void canWakeUpFromSleep() {
		if (mBindService != null) {
			if (mBindService.getCurrentState() == CoreService.ServiceState.STATE_RESUME) {
//				mBindService.wakeUpDevice(WorkingMode.MODE_INTERACTING);
//				finish();
				HelperUtil.finishActivity(this, android.R.anim.fade_out, android.R.anim.fade_out);
			} else {
				if (TSDConst.buildForDevice) {
					showText("小宝正在休息, 请勿打扰.");
				} else {
					if (++mClickdTimes == 1) {
						showText("小宝正在休息, 请勿打扰.");
					} else if (mClickdTimes > 2 && mClickdTimes < 5) {
						String text = String.format("再连续点击%d次即可唤醒小宝", (5-mClickdTimes));
						showText(text);
					} else if (mClickdTimes >= 5) {
						finish();
//						HelperUtil.finishActivity(this, android.R.anim.fade_out, android.R.anim.fade_in);
					}
				}
			}
		}
	}

	private void showText(String text) {
		if (mToast == null) {
			mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		} else {
			mToast.setText(text);
		}
		mToast.show();
	}

	private void updateTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
		String time = sdf.format(new Date(System.currentTimeMillis()));
		if (mTimeView != null) {
			mTimeView.setText(time);
		}
	}

	private class TimeTask extends Thread {
		private boolean stop;

		@Override
		public void run() {
			Looper.getMainLooper();
			while (!stop) {
				Message msg = Message.obtain(null, 100);
				mHandler.sendMessage(msg);
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
			}
		}	
	}

	private void setWeatherData(GetWeatherRes data) {
		if (data != null && data.errorCode == 0) {
			if (mWeatherTextView != null) {
				mWeatherTextView.setText(data.weather.text);
			}
			if (mHighTempView != null) {
				mHighTempView.setText(data.weather.temperatureHigh);
			}
			if (mLowTempView != null) {
				mLowTempView.setText(data.weather.temperatureLow);
			}

//			TypedArray icons = mContext.getResources().obtainTypedArray(R.array.weather_icons_array);
//			if (mImgView != null) {
//				Drawable d = icons.getDrawable(data.weather.code);
//				mImgView.setImageDrawable(d);
//			}
//			icons.recycle();

//			Suggestion[] suggestions = data.weather.suggestions;
//			if (suggestions != null) {
//				for (Suggestion s : suggestions) {
//					if (s.name.equals("car_washing")) {
//						mSugestView.setText(s.brief + "洗车");
//						if (s.brief.equals("非常适宜") || s.brief.equals("适宜") || s.brief.equals("比较适宜")) {
//							mSugestView.setVisibility(View.VISIBLE);
//							mSugestView.setBackground(R.drawable.recommend_bg);
//						} else if (s.brief.equals("不太适宜") || s.brief.equals("不适宜")){
//							mSugestView.setVisibility(View.VISIBLE);
//							mSugestView.setBackground(R.drawable.not_recommend_bg);							
//						} else {
//							mSugestView.setVisibility(View.GONE);
//						}
//						break;
//					}
//				}
//			}
		}
	}
}
