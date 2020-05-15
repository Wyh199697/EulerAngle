package com.example.eulerangle;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends Activity {

    private static final String TAG = "sensor";
    double[] accelerometerValues = new double[3];
    double[] magneticFieldValues = new double[3];
    double[] linearaccelerometerValues = new double[3];
    private SensorManager sm;
    //需要两个Sensor
    private Sensor aSensor;
    private Sensor mSensor;
    private Sensor lSensor;
    private TextView textView1, textView2, textView3, textView4, textView5, textView6, textView7, textView8;
    long start, end;
    int count = 0;
    int win_size = 25;
    double[] win_a = new double[win_size];
    long[] delta_time = new long[win_size];
    double[] o_a = new double[win_size];
    double m = (double) 0.1;
    double n = (double) 0.05;
    double o = (double) 0.0001;
    double x_z = 0;
    double x = x_z, p = 0.01d, k;
    double q = 0.25d;
    double r = 0.01d;
    double[] vel = new double[win_size];
    double[] x_arr = new double[win_size];
    double pos = 0, v_pre = 0, a_pre;
    long f, s;
    int c;
    double v = 0d;
    double total = 0;
    int debug = 0;
    long delta_t = 4;
    //final Context context;
    DecimalFormat decimalFormat = new DecimalFormat("0.0000");
    Queue<Double> queue = new LinkedList<>();
    FileOutputStream fileOutputStream = null;
    BufferedWriter bufferedWriter = null;
    int r_count = 0;
    final SensorEventListener myListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {

            /*if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
                count++;
                if(end - start >= 1000) {
                    start = System.currentTimeMillis();
                    count = 0;
                }
                linearaccelerometerValues = sensorEvent.values;
                end = System.currentTimeMillis();
                calculateOrientation();
                Log.d(TAG, "Start: " + start);
                Log.d(TAG, "End: " + end);
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
                count++;
                if(end - start >= 1000) {
                    start = System.currentTimeMillis();
                    count = 0;
                }
                linearaccelerometerValues = sensorEvent.values;
                end = System.currentTimeMillis();
                calculateOrientation();
                Log.d(TAG, "Start: " + start);
                Log.d(TAG, "End: " + end);*/
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                end = System.currentTimeMillis();
                c++;
                if(s - f >= 1000){
                    f = System.currentTimeMillis();
                    textView5.setText("count: " + c);
                    c = 0;
                }
                float[] aa = sensorEvent.values;
                for(int i = 0; i < 3; i++){
                    linearaccelerometerValues[i] = Double.valueOf(String.valueOf(aa[i]));
                    Log.d(TAG, "asdasdasd:" + linearaccelerometerValues[i] + " " + aa[i]);
                }
                calculateOrientation();
                s = System.currentTimeMillis();
                start = end;
            }

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);

        textView4 = findViewById(R.id.textView4);
        textView5 = findViewById(R.id.textView5);
        textView6 = findViewById(R.id.textView6);
        textView7 = findViewById(R.id.textView7);
        textView8 = findViewById(R.id.textView8);
        Button button = findViewById(R.id.bottom);
        /*button.setOnClickListener((v)->{

        });*/

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sm = getSensorManager(context);
        //aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        lSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //sm.registerListener(myListener, aSensor, 0);
        //sm.registerListener(myListener, mSensor, 0);
        sm.registerListener(myListener, lSensor, SensorManager.SENSOR_DELAY_FASTEST);
        //更新显示数据的方法
        //if (debug == 1) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
                Date date = new Date();
                fileOutputStream = openFileOutput(sdf.format(date) + ".csv", Context.MODE_PRIVATE);
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
                bufferedWriter.write("origin_acc,x,win_a,vel,pos,delta_t\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        //}

        calculateOrientation();
        start = System.currentTimeMillis();
        Button button1 = findViewById(R.id.button);
        button1.setOnClickListener(v->{
            if(debug == 1) {
                debug = 0;
            }else{
                debug = 1;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //再次强调：注意activity暂停的时候释放
    public void onPause() {
        sm.unregisterListener(myListener);
        super.onPause();
        if(debug == 1) {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void calculateOrientation() {
        //double[] values = new double[3];
        double[] l_a = linearaccelerometerValues;
        /*double t_a = Math.sqrt(l_a[0]*l_a[0]+l_a[1]*l_a[1]+l_a[2]*l_a[2]);
        if(l_a[1] < 0){
            t_a = -t_a;
        }*/
        /*double[] R = new double[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);

        // 要经过一次数据格式的转换，转换为度
        values[0] = (double) Math.toDegrees(values[0]);
        textView1.setText(Double.toString(values[0]));
        values[1] = (double) Math.toDegrees(values[1]);
        textView2.setText(Double.toString(values[1]));
        values[2] = (double) Math.toDegrees(values[2]);
        textView3.setText(Double.toString(values[2]));*/

        /*if(Math.abs(l_a[0]) < 0.01){
            x = 0;
        }else {
            x = l_a[0];
        }
        if(Math.abs(l_a[1]) < 0.01){
            y = 0;
        }else {
            y = l_a[1];
        }
        if(Math.abs(l_a[2]) < 0.01){
            z = 0;
        }else {
            z = l_a[2];
        }
        v += x * (end - start) /1000;
        d += v * (end - start)/1000;
        textView4.setText("x:" + x);
        textView5.setText("y:" + y);
        textView6.setText("z:" + z);*/
        /*
        x = x
        p = p

        k = p * (p + 0.0025)
        p = (1 - k) * p
        x = x + k * (a - x)
     */
        double g = 0;
        double s = 0;
        x = x;
        p = p;
        k = (double) (p * (p + r));

        x = x + k * (l_a[1] - x);
        p = (1 - k) * p + q;

        if(count != win_size){
            win_a[count] = x;
            x_arr[count] = x;
            o_a[count] = l_a[1];
            if(count == 1 || count == 0) {
                delta_time[count] = end - start;
            }else{
                delta_time[count] = end - start;
            }
            count++;
        }else{
            g = GetDataEx(win_a);
            s = StandardDiviation(win_a);
            if(g < m && s < n) {
                win_a = new double[win_size];
                //v = 0;
            }else if(s >= n){
                Log.d(TAG, "calculateOrientation: ");
            }
            for(int i = 0; i < win_size; i++) {
                //threaddelay();
                textView1.setText("x: " + win_a[i]*100);
                //Log.d(TAG, "win_a:" + win_a[i]);
            }
            double sum = 0;
            vel[0] = v_pre + a_pre * (double)delta_t / 1000d;
            for(int i = 1; i < win_size; i++){
                vel[i] = vel[i-1] + (win_a[i] * (double)delta_t / 1000d);
                sum += (win_a[i-1] * (double)delta_t / 1000d);
                Log.d(TAG, "jiange: " + (end - start) + " zengling: " + ((win_a[i-1] * delta_time[i]) / 1000)*100 + "cm/s2 v_pre: " + v_pre*100 + "cm/s");
                Log.d(TAG, "win_a: " + win_a[i-1]);
                //pos += vel * delta_time[i] / 1000;
            }
            for(int i = 1; i < win_size; i++){
                if(vel[i-1] != vel[i]) {
                    Log.d(TAG, "vel[]" + i + ": " + vel[i] * 100);
                }
            }
            if(sum != 0){
                total += sum;
            }
            Log.d(TAG, "sum: " + sum*100);
            if(sum == 0.0) {
                Log.d(TAG, "total: " + total * 100);
                total = 0;
            }
            double v_s = StandardDiviation(vel);
            if(v_s < 0.0001){
                vel = new double[win_size];
                Log.d(TAG, "asdasd");
            }else{

            }
            for(int i = 0; i < win_size; i++) {
                textView4.setText("vel: " + vel[i] * 100 + "cm/s\nv_s: " + decimalFormat.format(v_s));
                Log.d(TAG, "vel: " + vel[i]);
            }
            pos += v_pre*delta_t/1000d;
            for(int i = 1; i < win_size; i++){
                pos += vel[i]*(double)delta_t/1000d;
                Log.d(TAG, "qwe:" + o_a[i] + "," + x_arr[i] + "," + win_a[i] + "," + vel[i] + "," + pos + "\n");
                if(debug == 1) {
                    try {
                            bufferedWriter.write(o_a[i] + "," + x_arr[i] + "," + win_a[i] + "," + vel[i] + "," + pos + "," + (double)delta_time[i]/1000 + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            count = 1;



            textView3.setText("g: " + g + "\ns: " + s);
            Log.d(TAG, "g: " + g + " s: " + s);
            v_pre = vel[win_size-1];
            a_pre = win_a[win_size-1];
            win_a[0] = x;
            x_arr[0] = x;
            o_a[0] = l_a[1];
            delta_time[0] = end - start;
        }

        v = v + x * ((double)delta_t)/1000d;
        textView6.setText("vvv: " + v*100 + "cm/s");

        textView2.setText("x:" + l_a[0]*100 + "\ny:" + l_a[1]*100 + "\nz:" + l_a[2]*100 + "\ng:" + Math.sqrt(l_a[0]*l_a[0]+l_a[1]*l_a[1]+l_a[2]*l_a[2]) + "\njiange: " + (end - start));
        textView7.setText("pos: " + pos*100 + "cm");

        Log.d(TAG, "l_a - x: " + (l_a[1] - x));
        Log.d(TAG, "k: " + k);
        Log.d(TAG, "p: " + p);
        Log.d(TAG, "xx: " + x);
        Log.d(TAG, "l_a: " + l_a[1]);
        Log.d(TAG, "k * (l_a[1] - x: " + (k * (l_a[1] - x)));


    }

    public static double GetDataEx(double[] mSourceData) {
        double num, sum;
        int i, j;
        List<Double> list = new ArrayList<>();
        for (i = 0; i < mSourceData.length; i++) {
            num = mSourceData[i];
            if (list.size() == 0) {
                list.add(num);
                list.add(1d);
            } else {
                for (j = 0; j < list.size(); j+=2) {
                    if (list.get(j) == num) {
                        list.set(j+1, list.get(j+1)+1);
                        break;
                    }
                }
                if (j >= list.size()) {
                    list.add(num);
                    list.add(1d);
                }
            }
        }
        i = mSourceData.length;
        double sum2 = 0f;
        for (j = 0; j < list.size(); j+=2) {
            sum2 += list.get(j)*list.get(j+1)/(double)i;
        }
        sum = (double) sum2;
//        mSourceData.clear();
        return sum;
    }

    //标准差σ=sqrt(s^2)
    public static double StandardDiviation(double[] x) {
        int m=x.length;
        double sum=0;
        for(int i=0;i<m;i++){//求和
            sum+=x[i];
        }
        double dAve=sum/m;//求平均值
        double dVar=0;
        for(int i=0;i<m;i++){//求方差
            dVar+=(x[i]-dAve)*(x[i]-dAve);
        }
        //reture Math.sqrt(dVar/(m-1));
        return (double)Math.sqrt(dVar/m);
    }

    public static void threaddelay(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
}
