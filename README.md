# Remoterobocar-Android

This application is used to control Robocar via Bluetooth.

- MinSdk: 23
- TargetSdk: 32

[Robocar Arduino Progam](https://github.com/Huysiunhan02/Robocar_Arduino.git)
## Preview

<img src="https://github.com/Huysiunhan02/Remoterobocar/assets/96275325/884785cd-b54b-4177-be62-93ae298dd83f" width="250" alt="Preview" />


#### Note: Synchronize receiving and sending data
Default: The character that marks the end of a send command is "#"

Edit it in functions:
```java
public void sendMessage(String sMessage) {
        if (BTSocket != null && iBTConnectionStatus == BT_CON_STATUS_CONNECTED) {
            if (BTSocket.isConnected()) {
                try {
                    sMessage += "#";//kí tự đánh dấu kết thúc một chuỗi được gửi đi
                    cBTInitSendReceive.write(sMessage.getBytes());
                } catch (Exception exp) {

                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please connect to bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
```
