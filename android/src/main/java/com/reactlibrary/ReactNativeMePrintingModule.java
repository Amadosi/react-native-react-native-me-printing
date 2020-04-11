package com.reactlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.newland.me.ConnUtils;
import com.newland.me.DeviceManager;
import com.newland.mtype.ConnectionCloseEvent;
import com.newland.mtype.ModuleType;
import com.newland.mtype.event.DeviceEventListener;
import com.newland.mtype.module.common.printer.PrintContext;
import com.newland.mtype.module.common.printer.Printer;
import com.newland.mtype.module.common.printer.PrinterResult;
import com.newland.mtype.module.common.printer.PrinterStatus;
import com.newland.mtypex.nseries3.NS3ConnParams;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReactNativeMePrintingModule extends ReactContextBaseJavaModule implements DeviceEventListener<ConnectionCloseEvent> {

    private static ReadableMap optionMap;

    private final ReactApplicationContext reactContext;

    private DeviceManager deviceManager;

    private Promise printPromise;

    public ReactNativeMePrintingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        deviceManager = ConnUtils.getDeviceManager();
        deviceManager.init(this.reactContext, "com.newland.me.K21Driver", new NS3ConnParams(),this);
    }

    @Override
    public String getName() {
        return "ReactNativeMePrinting";
    }

    @ReactMethod
    public static void config(ReadableMap options){
        optionMap = options;

        //String paymentUri = options.getString("payment_uri");
    }

    @ReactMethod
    public void print(String amount, String status, final Promise promise) {
        String PRINT_ERR = "PRINT_ERR";

        Printer printer;

        printPromise = promise;

        try {
            deviceManager.connect();
        } catch (Exception e) {
            e.printStackTrace();
            rejectPromise(PRINT_ERR, e.getMessage());
        }

        try{
            printer = (Printer) deviceManager.getDevice().getStandardModule(ModuleType.COMMON_PRINTER);
            if (printer.getStatus() != PrinterStatus.NORMAL) {
                rejectPromise(PRINT_ERR, "Print failed!Printer status is not normal");
            }else {
                Date date = new Date();
                Map<String, Bitmap> map = new HashMap<>();
                StringBuilder scriptBuffer = new StringBuilder();
//                Bitmap bitmap = BitmapFactory.decodeResource(this.reactContext.getResources(), R.drawable.logo);
//                map.put("logo", bitmap);
                scriptBuffer.append("*image l 370*100 path:");
                scriptBuffer.append("logo\n");
                scriptBuffer.append("!asc n\n"+"!hz n\n");
                scriptBuffer.append("!yspace 5\n");
                scriptBuffer.append("*line" + "\n");
                scriptBuffer.append("*text c (").append(status.toUpperCase()).append(" - Z1)\n");

                //Make sure that all the dynamic varialbes you want to print is always populated or else printer returns an error...
                scriptBuffer.append("*text l Time Stamp: ").append(date).append("\n");
                scriptBuffer.append("*text l " + "Amount: ").append(amount).append("\n");
                scriptBuffer.append("!yspace 5\n");
                scriptBuffer.append("*line" + "\n");
                scriptBuffer.append("*text c THANK YOU\n");

                //add footer
                if(optionMap.getString("footer_text") != null){
                    scriptBuffer.append("*line" + "\n");
                    scriptBuffer.append("*text c ").append(optionMap.getString("footer_text")).append("\n");
                }

                scriptBuffer.append("*feedline 3\n");

                PrinterResult printerResult = printer.printByScript(PrintContext.defaultContext(), scriptBuffer.toString(), map, 60, TimeUnit.SECONDS);

                if (printerResult == PrinterResult.SUCCESS) {
                    promise.resolve(null);
                } else {
                    rejectPromise(PRINT_ERR,"Print script failed! ");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            rejectPromise(PRINT_ERR,e.getMessage());
        }
    }

    @Override
    public void onEvent(ConnectionCloseEvent connectionCloseEvent, Handler handler) {

        String DEVICE_ERR_DISCONNECTED = "DEVICE_ERR_DISCONNECTED";

        if (connectionCloseEvent.isSuccess()) {
            rejectPromise(DEVICE_ERR_DISCONNECTED, "Device is disconnected by customers!");
        }
        if (connectionCloseEvent.isFailed()) {
            rejectPromise(DEVICE_ERR_DISCONNECTED, "Device is disconnected abnormally!");
        }
    }

    @Override
    public Handler getUIHandler() {
        return null;
    }

    private void rejectPromise(String code, String message) {

        Log.v("isPrinter","Print failed!Printer status is not normal");

        if (this.printPromise != null) {
            this.printPromise.reject(code, message);
            this.printPromise = null;
        }
    }
}
