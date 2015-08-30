package com.zyhscut.baymin;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;

import org.json.JSONException;
import org.json.JSONObject;

import android.R.interpolator;
import android.R.string;
import android.app.Activity;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

public class MainActivity extends Activity implements HttpGetDataListener, android.view.View.OnClickListener{

	private HttpData httpData;
	private List<ListData> lists;
	private ListView lv;
	private EditText sendText;
	private Button send_btn;
	private Button voice_btn;
	private String content_str;
	private TextAdapter adapter;
	private String[] welcome_array;
	private double currentTime, oldTime = 0;
	private RecognizerDialog mIatDialog;
	private SpeechSynthesizer mTts;
	private String TAG = "shitou";
	private String voicer="vinn";
	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;
	// 用HashMap存储听写结果
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
		lv = (ListView) findViewById(R.id.lv);
		sendText = (EditText) findViewById(R.id.sendText);
		send_btn = (Button) findViewById(R.id.send_btn);
		voice_btn = (Button) findViewById(R.id.voice_btn); 
    	lists = new ArrayList<ListData>();
    	send_btn.setOnClickListener(this);
    	voice_btn.setOnClickListener(this);
    	adapter = new TextAdapter(lists, this);
    	lv.setAdapter(adapter);
    	String randomWelcomeTips = getRandomWelcomeTips();
    	ListData listData;
    	listData = new ListData(randomWelcomeTips, ListData.RECEIVER,getTime());
    	lists.add(listData);
    	
    	SpeechUtility.createUtility(this, "appid=557aceb9");
    	
    	mIatDialog = new RecognizerDialog(this, mInitListener);
    	setIatParam();
    	
    	mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
    	setTtsParam();
		mTts.startSpeaking(randomWelcomeTips, mTtsListener);
	}
    
    private String getRandomWelcomeTips() {
		String welcome_tip = null;
		welcome_array = this.getResources().getStringArray(R.array.welcome_tips);
		int index = (int) (Math.random()*(welcome_array.length-1));
		welcome_tip = welcome_array[index];
		return welcome_tip;
	}
    
	@Override
	public void getDataUrl(String data) {
		//System.out.println(data);
		parseText(data);
	}

	public void parseText(String str) {
		try {
			JSONObject jb = new JSONObject(str);
//			System.out.println(jb.getString("code"));
//			System.out.println(jb.getString("text"));
			String text = jb.getString("text");
			ListData listData;
			listData = new ListData(text,ListData.RECEIVER,getTime());
			lists.add(listData);
			adapter.notifyDataSetChanged();
    		mTts.startSpeaking(text, mTtsListener);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}

	@Override
	public void onClick(View arg0) {
		content_str = "";
		if (arg0.getId() == R.id.send_btn) {
			content_str = sendText.getText().toString();
			sendText.setText("");
			sendContent(content_str);
		} 
		if (arg0.getId() == R.id.voice_btn) {
			mIatDialog.setListener(recognizerDialogListener);
			mIatDialog.show();
//			int ret = mIat.startListening(recognizerListener);
//			Log.d(TAG, "SpeechRecognizer ret:" + ret);
		}	
	}
	
	private String getTime() {
		currentTime = System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat("MM月dd日 hh:mm");
		Date curDate = new Date((long) currentTime);
		String str = format.format(curDate);
		if (currentTime - oldTime >= 300000) {
			oldTime = currentTime;
			return str;
		}else {
			return "";
		}
		
	}
	
	private void sendContent(String content_str) {
		if (!content_str.equals("")) {
			String dropk = content_str.replace(" ", "");
			String droph = dropk.replace("\n", "");
			ListData listData;
			listData = new ListData(content_str,ListData.SEND,getTime());
			lists.add(listData);
			if (lists.size()>30) {
				for (int i = 0; i < 10; i++) {
					lists.remove(0);
				}
			}
			adapter.notifyDataSetChanged();
			httpData = (HttpData) new HttpData(
					"http://www.tuling123.com/openapi/api?key=8f57ba49bb7d13c44599c8b0cd430773&info="+droph, 
					this).execute();
			}
	}
	
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
		}
	};
	
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        	} else {
				// 初始化成功，之后可以调用startSpeaking方法
        		// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
        		// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}		
		}
	};
	
	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	public void setIatParam() {
		mIatDialog.setParameter(SpeechConstant.DOMAIN, "iat");
		mIatDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		// 设置语言区域
		mIatDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
	}
	
	private void setTtsParam(){
		// 设置发音人
		mTts.setParameter(SpeechConstant.VOICE_NAME,voicer);
		//设置合成语速
		mTts.setParameter(SpeechConstant.SPEED,"80");
		//设置合成音量
		mTts.setParameter(SpeechConstant.VOLUME,"100");
		//设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE,"3");
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
	}
	
	/**
	 * 听写监听器。
	 */
//	private RecognizerListener recognizerListener = new RecognizerListener() {
//
//		@Override
//		public void onBeginOfSpeech() {
//		}
//
//		@Override
//		public void onError(SpeechError error) {
//		}
//
//		@Override
//		public void onEndOfSpeech() {
//		}
//
//		@Override
//		public void onResult(RecognizerResult results, boolean isLast) {
//			Log.d(TAG, results.getResultString());
//			collectResult(results);
//
//			if (isLast) {
//				// TODO 最后的结果
//				printResult(mIatResults);
//			}
//		}
//
//		@Override
//		public void onVolumeChanged(int volume) {
//		}
//
//		@Override
//		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
//		}
//	};
	
	/**
	 * 听写UI监听器
	 */
	private RecognizerDialogListener recognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
			collectResult(results);

			if (isLast) {
				// TODO 最后的结果
				printResult(mIatResults);
			}
		}

		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {
		}

	};
	
	private void collectResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());

		String sn = null;
		// 读取json结果中的sn字段
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mIatResults.put(sn, text);
		
	}
	
	private void printResult(HashMap<String, String> mIatResults) {
		
		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
			}
		content_str = resultBuffer.toString();
		sendContent(content_str);
		mIatResults.clear();
		
	}
	
	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {
		@Override
		public void onSpeakBegin() {
		}

		@Override
		public void onSpeakPaused() {
		}

		@Override
		public void onSpeakResumed() {
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
				String info) {
			// 合成进度
			mPercentForBuffering = percent;
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			mPercentForPlaying = percent;
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
			} else if (error != null) {
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			
		}
	};
}
