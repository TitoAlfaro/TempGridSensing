package mit.edu.obmg.tempgridsensing;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.R.bool;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ToggleButton;

public class TempGridMain extends IOIOActivity{
	private final String TAG = "TempGridSensingMain";
	private ToggleButton button_;
	
	//Sensor I2C
	private TwiMaster twiEeprom, twiYtai;
	private TwiMaster twiTemp;
	double sensortemp;
		
	//MultiThreading
	private Thread TempRead;
	Thread thread = new Thread(TempRead);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_temp_grid_main);
		button_ = (ToggleButton) findViewById(R.id.button);
	}

	
	class Looper extends BaseIOIOLooper {
		/** The on-board LED. */
		private DigitalOutput led_;
				
		@Override
		protected void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(0, true);

			//twiEeprom = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_400KHz, false);
			//twiTemp = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_1MHz, false);
			
			twiYtai = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_400KHz, false);
			
			try {
				TempRead thread_ = new TempRead(ioio_);
				thread_.start();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		public void loop() throws ConnectionLostException {

			led_.write(!button_.isChecked());
			
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
	
	class TempRead extends Thread{
    	private DigitalOutput led;
    	
    	private IOIO ioio_;
    	
    	public TempRead(IOIO ioio)throws InterruptedException{
    		ioio_ = ioio;
    	}
    	
    	public void run(){
    		super.run();
			//checkAddress(twiEeprom);
			while (true) {
				try{

				Thread.sleep(5);
				
				ReadEeprom(0x50, twiEeprom);
				//ReadSensor(0x60, twiTemp);
				
				} catch (Exception e) {
					Log.e("HelloIOIOPower", "Unexpected exception caught", e);
					ioio_.disconnect();
					break;
				}
			}
    	}
    }
	
	public void ReadSensor(int address, TwiMaster port) {
		
		byte[] request = new byte[] { 0x02, 0x00, 0x01 };	//Byte address to ask for sensor data
		byte[] response = new byte[0x40];		//Byte to save sensor data
		double dump;							//Value after processing sensor data

		try {
			Log.d(TAG, ":| Trying to read");
			port.writeRead(address, false, request,request.length,response,response.length);
			
			for(int i=0; i<response.length; i++){
				//dump = (double)(((response[i+1] & 0x007f) << 8)+ response[i]);
				Log.i(TAG, "EEPROM Dump "+i+" = "+ response[i]);
			}

			Log.d(TAG, ":) success reading");

		} catch (ConnectionLostException e) {
			Log.d(TAG, ":( read ConnLost");
			e.printStackTrace();
		} catch (InterruptedException e) {
			Log.d(TAG, ":( read InterrExcept");
			e.printStackTrace();
		}
	}
	
	public void ReadEeprom(int address, TwiMaster port) {
		
		byte[] request = new byte[] { 0x00 };	//Byte address to ask for sensor data
		byte[] response = new byte[1];		//Byte to save sensor data

		byte[] getData = new byte[] { 0x50 };	//Byte address to ask for sensor data
		byte[] dataDump = new byte[0xff];		//Byte to save sensor data

		//try {
			Log.d(TAG, ":| Trying to read");
			
			

				//Then, you can call for example:
		
				byte[] resultYtai = new byte[125];
				if (readEepromYtai(0x00, resultYtai)) {
					for(int i=0; i<resultYtai.length; i++){
						Log.i(TAG, "EEPROM Dump "+i+" = "+ resultYtai[i]);
					}
				}
				
				byte[] resultYtai2 = new byte[125];
				if (readEepromYtai(125, resultYtai2)) {
					for(int i=0; i<resultYtai2.length; i++){
						Log.i(TAG, "EEPROM Dump2 "+ (i+resultYtai2.length) +" = "+ resultYtai2[i]);
					}
				}
			
			/*if (port.writeRead(address, false, request,request.length,response,response.length)){
				Log.i(TAG, ":) Got Initial response");		
				for(int i=0; i<response.length; i++){
					//dump = (int)(((response[i] & 0x007f) << 8)+ response[i]);
					Log.i(TAG, "EEPROM Dump "+i+" = "+ response[i]);
					if(i == 0xf7){
						Log.i(TAG, "Triming Value = "+ response[i]);
					}
				}
			}else{
				Log.i(TAG, ":( NO response");				
			}*/
			
			/*if (port.writeRead(address, false, request,request.length,response,response.length)){
				Log.i(TAG, ":) Got Initial response");
				
				if (port.writeRead(address, false, getData,getData.length,dataDump,dataDump.length)){
					Log.i(TAG, ":) Got Data response");
					
					for(int i=0; i<dataDump.length; i++){
					//dump = (int)(((response[i] & 0x007f) << 8)+ response[i]);
					Log.i(TAG, "EEPROM Dump "+i+" = "+ dataDump[i]);
					}
				}
			}*/
			
			Log.d(TAG, ":) success reading");

		/*} catch (ConnectionLostException e) {
			Log.d(TAG, ":( read ConnLost");
			e.printStackTrace();
		} catch (InterruptedException e) {
			Log.d(TAG, ":( read InterrExcept");
			e.printStackTrace();
		}*/
	}

	boolean readEepromYtai(int start_address, byte[] result) {
		  byte requestYtai[] = new byte[] { (byte) start_address };
		  try {
			return twiYtai.writeRead(0x50, false, requestYtai, 1, result, result.length);
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
	
	public void checkAddress(TwiMaster port){
		Log.i(TAG, ":| Checking Addresses...");
		byte[] request_on = new byte[] { 0x00 };
		byte[] response = new byte[1];
		for(int i=0; i<255; i++){

			try {
				if( port.writeRead(i, false, request_on,request_on.length,response,response.length)){
					Log.i(TAG, ":)  Address "+ i+ " works!");
				}else{
					Log.i(TAG, ":(  Address "+ i+ " doesn't work!");
				}
			} catch (ConnectionLostException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, ":(  Address "+ i+ " doesn't work! Connection Lost");
				e.printStackTrace();
			} catch (InterruptedException e) {

				Log.i(TAG, ":(  Address "+ i+ " doesn't work! Interrupted Exception");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onStop() {
		//twiEeprom.close();
		twiYtai.close();
		super.onStop();
	}
}
