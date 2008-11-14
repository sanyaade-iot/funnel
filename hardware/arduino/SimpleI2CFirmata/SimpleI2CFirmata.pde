#include <Wire.h>
#include <Firmata.h>

#define ENABLE_POWER_PINS
#define SYSEX_I2C 0x76
#define I2C_WRITE 0
#define I2C_READ 1
#define I2C_READ_CONTINUOUSLY 2
#define I2C_STOP_READING 3

unsigned long currentMillis;     // store the current value from millis()
unsigned long nextExecuteMillis; // for comparison with currentMillis

byte slaveAddress;
byte slaveRegister;
byte i2cRxData[32];
boolean readingContinuously = false;

void readAndReportData(byte address, byte theRegister, byte numBytes) {
  Wire.beginTransmission(address);
  Wire.send(theRegister);
  Wire.endTransmission();

  Wire.requestFrom(address, numBytes);

  while (Wire.available() < numBytes) {
    // TODO: Do timeout here if needed
  }

  i2cRxData[0] = address;
  i2cRxData[1] = theRegister;
  for (int i = 0; i < numBytes; i++) {
    i2cRxData[2 + i] = Wire.receive();
  }

  // send slave address, register and received bytes
  Firmata.sendSysex(SYSEX_I2C, numBytes + 2, i2cRxData);
}

void sysexCallback(byte command, byte argc, byte *argv)
{
  byte mode;
  int i;
  byte data;

  if (command == SYSEX_I2C) {
    mode = argv[0];
    slaveAddress = argv[1];

    switch(mode) {
    case I2C_WRITE:
      Wire.beginTransmission(slaveAddress);
      for (i = 2; i < argc; i += 2) {
        data = argv[i] + (argv[i + 1] << 7);
        Wire.send(data);
      }
      Wire.endTransmission();
      delayMicroseconds(70);
      break;
    case I2C_READ:
      slaveRegister = argv[2] + (argv[3] << 7);
      data = argv[4] + (argv[5] << 7);  // bytes to read
      readAndReportData(slaveAddress, slaveRegister, data);
      break;
    case I2C_READ_CONTINUOUSLY:
      // TODO: implement here
      readingContinuously = true;
      break;
    case I2C_STOP_READING:
      // TODO: implement here
      readingContinuously = false;
      break;
    default:
      break;
    }
  }
}

// reference: BlinkM_funcs.h by Tod E. Kurt, ThingM, http://thingm.com/
static void enablePowerPins(byte pwrpin, byte gndpin)
{
  DDRC |= _BV(pwrpin) | _BV(gndpin);
  PORTC &=~ _BV(gndpin);
  PORTC |=  _BV(pwrpin);
  delay(100);
}

void setup()
{
  Firmata.setFirmwareVersion(2, 0);

  Firmata.attach(START_SYSEX, sysexCallback);

  for (int i = 0; i < TOTAL_DIGITAL_PINS; ++i) {
    pinMode(i, OUTPUT);
  }

#ifdef ENABLE_POWER_PINS
  // AD2, AD3, AD4, AD4
  // GND, PWR, SDA, SCL: e.g. BlinkM, HMC6352
  enablePowerPins(PC3, PC2);
#endif

  // It seems that Arduino Pro Mini won't work with 115200bps
  if (F_CPU == 8000000) {
    Firmata.begin(19200);
  } else {
    Firmata.begin(115200);
  }

  Wire.begin();
}

void loop()
{
  while (Firmata.available()) {
    Firmata.processInput();
  }

  currentMillis = millis();
  if(currentMillis > nextExecuteMillis) {  
    nextExecuteMillis = currentMillis + 19; // run this every 20ms

    // TODO: read continuously and report here if requested
  }
}
