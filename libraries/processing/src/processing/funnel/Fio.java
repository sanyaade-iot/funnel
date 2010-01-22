package processing.funnel;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;

import com.illposed.osc.OSCMessage;


import processing.core.PApplet;


/**
 * @author endo
 * @version 1.2
 * 
 */
public final class Fio extends Firmata{
	
	public static final String moduleName = "fio";
	
	/**
	 * 0xFFFF : broadcast
	 */
	public static final int moduleID = 0xFFFF;//
	
	public static final int IN = PORT_DIN;
	public static final int OUT = PORT_DOUT;
	public static final int PWM = PORT_AOUT;
	
	private static final int[] firmata = {
		PORT_DOUT,PORT_DOUT,PORT_DOUT,PORT_AOUT,PORT_DOUT,PORT_AOUT,PORT_AOUT,
		PORT_DOUT,PORT_DOUT,PORT_AOUT,PORT_AOUT,PORT_AOUT,PORT_DOUT,PORT_DOUT,
		PORT_AIN,PORT_AIN,PORT_AIN,PORT_AIN,PORT_AIN,PORT_AIN,PORT_AIN,PORT_AIN,
	};
	public static final Configuration FIRMATA = new Configuration(moduleID,firmata,moduleName);
	

	/**
	 * Fio ポート番号からfunnelのポート番号への変換
	 */
	static final int[] _a = {14,15,16,17,18,19,20,21};
	static final int[] _d = {0,1,2,3,4,5,6,7,8,9,10,11,12,13};
	

	private HashSet<Integer> nodes = new HashSet<Integer>();
	private int[] IDs;
	private Configuration config;
	
	public Fio(PApplet parent, String hostName, String serverPortName,
			int commandPortNumber,int samplingInterval,int[] IDs,Configuration config){
		super(parent,hostName,serverPortName,commandPortNumber,samplingInterval,config);

		regModule(IDs,config);
		this.IDs = IDs;


		this.config = config;
		
		initPorts(_a,_d);

		startIOSystem();
		
		if(withoutServer){
			if(!initialize(config)){
				errorMessage("Funnel configuration error!");
			}else{
				thread = new Thread(this,"funnelServiceThread");
				thread.start();
			}
		}
		
	}
	
	public Fio(PApplet parent,int[] IDs,Configuration config){	
		this(parent,"localhost",null,CommandPort.defaultPort,
				33,IDs,config);
	}
	
	
	public Fio(PApplet parent, int samplingInterval,int[] IDs,Configuration config ){
		
		this(parent,"localhost",null,CommandPort.defaultPort,
				samplingInterval,IDs,config);
	}

	public Fio(PApplet parent, int samplingInterval,int[] IDs,Configuration config ,String serverPortName){
		
		this(parent,"localhost",serverPortName,CommandPort.defaultPort,
				samplingInterval,IDs,config);
	}
	
	public Fio(PApplet parent,
			int commandPortNumber,int samplingInterval,int[] IDs ,Configuration config,String serverPortName){
		
		this(parent,"localhost",serverPortName,commandPortNumber,
				samplingInterval,IDs,config);
	}
	
	protected boolean startIOSystem(){

		beginPolling();
		

		
		new NotifyTokenizer(this,client.commandPort);		
		
		return true;
	}

	//それぞれのエンドデバイス
	private void regModule(int[] IDs,Configuration config){
		System.out.println("module registerd  [Fio]");
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		
		for(int i=0;i<IDs.length;i++){
		
			String name = "Fio.ID" + nf.format(IDs[i]);
			addModule(IDs[i],config,name);
		}
	}
	
	protected void interpretMessage(OSCMessage message){
		
//		System.out.print("interpret " + message.getAddress() + "   ");
//		for(int i=0;i<message.getArguments().length;i++){
//			System.out.print(message.getArguments()[i] + "   " );
//		}
//		System.out.println( " " );
		
		if(message.getAddress().equals("/in") &&initialized){
			
			int id = ((Integer)message.getArguments()[0]).intValue();
			int n = ((Integer)message.getArguments()[1]).intValue();
			
			if(nodes.contains(id)){
				
				try{
					IOModule io = iomodules.get(id);
					for(int i=2;i<message.getArguments().length;i++){
						
						//入力ポートを更新する
						int nPort = n+i-2;
						io.pin(nPort).updateInput(((Float)message.getArguments()[i]).floatValue());
			
					}
				}catch(NullPointerException e){
					errorMessage("Not match your end device MY.");
				}			
			}

		}
		
		if(message.getAddress().equals("/node")){
			
			if(!initialized){
				int n = message.getArguments().length;

				if(n == 2){
					
//					String name = (String)message.getArguments()[1];
					
					int my = ((Integer)message.getArguments()[0]).intValue();
					if(Arrays.binarySearch(IDs, my)>=0){
						nodes.add(my);
					}
					
					
					if(nodes.size() == IDs.length){

						if(!initialize(config)){
							errorMessage("Funnel configuration error!");
						}else{
							thread = new Thread(this,"funnelServiceThread");
							thread.start();
						}
					}
				}
			}
	
		}


	}
	
	@Override
	protected void startingServer(String serverSerialName){
		waitingServer(moduleName,serverSerialName);
	}
	

}
