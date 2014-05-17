package mit.edu.obmg.tempgridsensing;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class TempGridMain extends IOIOActivity {
	private final String TAG = "TempGridSensingMain";

	// UI
	private ToggleButton button_;
	private TextView debugText;
	boolean debugFlag, eepromFlag = false;

	// Sensor I2C
	private TwiMaster twi;
	double sensortemp;

	//Sensor Data & render
	int[] irData = new int[64];
	DrawView drawView;
	Bitmap bg = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
	Canvas canvas = new Canvas(bg);
	final int rows = 4;
	final int cols = 16;
	float[] source = new float[cols*rows];
	float [][] dest = new float [4][16];
	
	// MultiThreading
	private Thread TempRead;
	Thread thread = new Thread(TempRead);

	@SuppressLint("NewApi") @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_temp_grid_main);
				
		button_ = (ToggleButton) findViewById(R.id.button);
		debugText = (TextView) findViewById(R.id.debugText);

		Paint paint = new Paint();
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.rect);
		rl.setBackgroundDrawable(new BitmapDrawable(bg));

	}

	private void DrawGrid() {
		Paint paint = new Paint();
		
		int colWidth = 30;
		int rowHeight = 90;
		
		//double for-loop to create Grid
		int k=0;
		for (int j=0; j<rows; j++){
			for(int i=0; i<cols; i++){
				if (k<irData.length){
					dest[j][i] = irData[k];
					Log.i(TAG, ":) Dest Value ("+i+","+j+")= " + dest[j][i]);
					float colorV = map(dest[j][i], 0, 100, 0, 255);
					paint.setColor(Color.argb(255,(int)colorV,50,50));
					canvas.drawRect(i*(colWidth+2), j*(rowHeight+3), (i*(colWidth+2))+colWidth, (j*(rowHeight+3))+rowHeight, paint);
					paint.setColor(Color.YELLOW); 
					paint.setTextSize(10); 
					canvas.drawText("V"+colorV,i*(colWidth+5), j*(rowHeight+45),paint);
					k++;
				}
			}
		}
	}

	class Looper extends BaseIOIOLooper {
		/** The on-board LED. */
		private DigitalOutput led_;

		@Override
		protected void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(0, true);

			twi = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_400KHz, false);

			try {
				TempRead thread_ = new TempRead(ioio_);
				thread_.start();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@SuppressLint("NewApi")
		@Override
		public void loop() throws ConnectionLostException {

			led_.write(!button_.isChecked());
			if (debugFlag) {
				debugText.post(new Runnable() {
					public void run() {
						debugText.setAlpha(0);
					}
				});
			} else {
				debugText.post(new Runnable() {
					public void run() {
						debugText.setAlpha(1);
					}
				});
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	class TempRead extends Thread {
		private DigitalOutput led;

		private IOIO ioio_;

		public TempRead(IOIO ioio) throws InterruptedException {
			ioio_ = ioio;
		}

		public void run() {
			super.run();
			// checkAddress(twiEeprom);
			while (true) {
				try {

					Thread.sleep(5);

					if (!eepromFlag) {
						ReadEeprom(0x50, twi);
					}
					ReadSensor(0x50, twi);
					DrawGrid();

				} catch (Exception e) {
					Log.e("HelloIOIOPower", "Unexpected exception caught", e);
					ioio_.disconnect();
					break;
				}
			}
		}
	}

	public void ReadSensor(int address, TwiMaster port) {

		int requestTempAddress = 0x00; // Byte address to ask for sensor data
		byte[] responseTemp = new byte[64]; // Byte to save sensor data

		sendDebugText(":| Trying to read Temp");
		Log.d(TAG, ":| Trying to read Temp");

		if (requestTemp(requestTempAddress, responseTemp)) {
			for (int i=0; i<63; i++){
				irData[i] = responseTemp[i];
				sendDebugText(":) Temp Value = " + irData[i]);
				Log.i(TAG, ":) Temp Value = " + irData[i]);
			}
		}
	}

	boolean requestTemp(int requestTempAddress, byte[] result) {

		//byte request[] = new byte[] { command, Start Address, Address Step, Number of Reads };
		byte request[] = new byte[] { 0x02, (byte) requestTempAddress, 0x01, 0x40 };
		try {
			return twi.writeRead(0x60, false, request, request.length, result,
					result.length);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public void ReadEeprom(int address, TwiMaster port) {

		int requestEeprom = 0x00; // Byte address to ask for sensor data
		byte[] responseEeprom1 = new byte[125]; // Byte to save sensor data
		byte[] responseEeprom2 = new byte[125]; // Byte to save sensor data
		int trimVal = 0;
		int configValF5 = 0;
		int configValF6 = 0;

		// try {
		sendDebugText(":| Trying to read Eeprom");
		Log.d(TAG, ":| Trying to read Eeprom");

		// Then, you can call for example:

		if (eepromRequest(requestEeprom, responseEeprom1)) {
			for (int i = 0; i < responseEeprom1.length; i++) {
				Log.i(TAG, "EEPROM Dump " + i + " = " + responseEeprom1[i]);
			}
		}

		if (eepromRequest(125, responseEeprom2)) {
			for (int i = 0; i < responseEeprom2.length; i++) {
				Log.i(TAG, "EEPROM Dump2 " + (i + responseEeprom2.length)
						+ " = " + responseEeprom2[i]);
				if (i + responseEeprom2.length == 0xF7) {
					trimVal = responseEeprom2[i];
					Log.i(TAG, "Trimming Value =" + responseEeprom2[i]);
				}
				if (i + responseEeprom2.length == 0xF5) {
					configValF5 = responseEeprom2[i];
					Log.i(TAG, "Config Value F5=" + responseEeprom2[i]);
				}
				if (i + responseEeprom2.length == 0xF6) {
					configValF6 = responseEeprom2[i];
					Log.i(TAG, "Config Value F6=" + responseEeprom2[i]);
				}
			}
		}

		sendDebugText(":) success reading Eeprom");
		sendDebugText("Trimming Value =" + trimVal);
		sendToast("Trimming Value = " + trimVal);
		Log.d(TAG, ":) success reading Eeprom");

		// Write Trim Value
		sendDebugText(":| Trying to write Trim Value");
		Log.d(TAG, ":| Trying to write Trim Value");

		if (writeTrimVal(trimVal)) {
			sendDebugText(":) Trim Value Written");
			Log.i(TAG, ":) Trim Value Written");

		} else {
			Log.i(TAG, ":( NO response");
		}

		// Read Oscillator trimming register
		sendDebugText(":| Trying to Read Oscillator trimming Register");
		Log.d(TAG, ":| Trying to Read Oscillator trimming Register");

		int requestOscTrim = 0x93; // Byte address to ask for sensor data
		byte[] responseOscTrim = new byte[1]; // Byte to save sensor data

		if (readOscTrim(requestOscTrim, responseOscTrim)) {
			sendDebugText(":) Oscillator trimming Register Read = "
					+ responseOscTrim);
			sendToast("Oscillator trimming Register = " + responseOscTrim[0]);
			Log.i(TAG, ":) Oscillator trimming Register Read = "
					+ responseOscTrim);

		} else {
			Log.i(TAG, ":( NO response");
		}

		// Write Configuration Values
		sendDebugText(":| Trying to write Configuration Value");
		Log.d(TAG, ":| Trying to write Configuration Value");

		if (writeConfigVal(configValF5, configValF6)) {
			sendDebugText(":) Configuration Value Written");
			sendToast("Configuration Value written= " + configValF5
					+ configValF6);
			Log.i(TAG, ":) Configuration Value Written");

		} else {
			Log.i(TAG, ":( NO response");
		}

		// Read Config. Register
		sendDebugText(":| Trying to Read Config. Register");
		Log.d(TAG, ":| Trying to Read Config. Register");

		int requestConfigReg = 0x92; // Byte address to ask for sensor data
		byte[] responseConfigReg = new byte[1]; // Byte to save sensor data

		if (readConfigReg(requestConfigReg, responseConfigReg)) {
			sendDebugText(":) Config. Register Read = " + responseConfigReg);
			sendToast("Configuration Value Read= " + responseConfigReg[0]);
			Log.i(TAG, ":) Config. Register Read = " + responseConfigReg);

		} else {
			Log.i(TAG, ":( NO response");
		}

		eepromFlag = true;
	}

	boolean eepromRequest(int start_address, byte[] result) {
		byte request[] = new byte[] { (byte) start_address };
		try {
			return twi
					.writeRead(0x50, false, request, 1, result, result.length);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	boolean writeTrimVal(int trimVal) {

		byte[] command = new byte[] { (byte) 0x60, 0x04,
				(byte) (trimVal - 0xAA), (byte) trimVal, 0x56, 0x00 };
		byte[] result = new byte[1];
		try {
			return twi.writeRead(0x50, false, command, command.length, result,
					result.length);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	boolean readOscTrim(int start_address, byte[] result) {
		byte request[] = new byte[] { (byte) start_address, 0x02 };
		try {
			return twi.writeRead(0x50, false, request, request.length, result,
					result.length);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	boolean writeConfigVal(int configF5, int configF6) {

		byte[] command = new byte[] { 0x60, 0x03, (byte) (configF5 - 0x55),
				(byte) configF5, (byte) (configF6 - 0x55), (byte) configF6 };

		byte[] result = new byte[1];
		try {
			return twi.writeRead(0x50, false, command, command.length, result,
					result.length);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	boolean readConfigReg(int start_address, byte[] result) {
		byte request[] = new byte[] { (byte) start_address, 0x02 };
		try {
			return twi
					.writeRead(0x50, false, request, request.length, result, result.length);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public void checkAddress(TwiMaster port) {
		Log.i(TAG, ":| Checking Addresses...");
		byte[] request_on = new byte[] { 0x00 };
		byte[] response = new byte[1];
		for (int i = 0; i < 255; i++) {

			try {
				if (port.writeRead(i, false, request_on, request_on.length,
						response, response.length)) {
					Log.i(TAG, ":)  Address " + i + " works!");
				} else {
					Log.i(TAG, ":(  Address " + i + " doesn't work!");
				}
			} catch (ConnectionLostException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, ":(  Address " + i
						+ " doesn't work! Connection Lost");
				e.printStackTrace();
			} catch (InterruptedException e) {

				Log.i(TAG, ":(  Address " + i
						+ " doesn't work! Interrupted Exception");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void onDebugClick(View view) {
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();

		if (on) {
			debugFlag = true;
		} else {
			debugFlag = false;
		}
	}

	private void sendDebugText(final String message) {

		debugText.post(new Runnable() {
			public void run() {
				debugText.setText(message);
			}
		});

	}

	private void sendToast(final String message) {

		debugText.post(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), message,
						Toast.LENGTH_SHORT).show();
			}
		});

	}
	
	float map(float x, float in_min, float in_max, float out_min, float out_max) {
		if (x < in_min)
			return out_min;
		else if (x > in_max)
			return out_max;
		else
			return (x - in_min) * (out_max - out_min) / (in_max - in_min)
					+ out_min;
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onStop() {
		// twiEeprom.close();
		if (twi != null)
			twi.close();
		super.onStop();
	}
}
