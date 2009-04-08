﻿package funnel{	import funnel.IOModule;	/**	 * This is the class to express I2C devices	 *	 */	public class I2CDevice implements ISysexMessageListener {		public static const I2C_REQUEST:uint = 0x76;		public static const I2C_REPLY:uint = 0x77;		public static const I2C_ADDRESS_BASE:uint = 128 * (I2C_REPLY + 2);		protected static const WRITE:uint = 0;		protected static const READ:uint = 1;		protected static const READ_CONTINUOUS:uint = 2;		protected static const STOP_READING:uint = 3;		protected var _io:IOModule;		private var _address:uint;		public function I2CDevice(ioModule:*, slaveAddress:uint) {			if (ioModule is IOModule) {				_io = ioModule;			} else if (ioModule is IOSystem) {				_io = ioModule.ioModule(0);			} else {				throw new ArgumentError("the first argument should be an IOModule or a IOSystem");				return;			}			_address = slaveAddress;			_io.addSysexListener(this);		}		public function update():void {			// To be implemented in sublasses		}		/**		 * @inheritDoc		 */		public function handleSysex(command:uint, data:Array):void {			// To be implemented in sublasses			// data should be: slave address, register, data0, data1...		}		/**		 * @inheritDoc		 */		public function get command():uint {			return I2C_ADDRESS_BASE + _address;		}		public function get address():uint {			return _address;		}	}}