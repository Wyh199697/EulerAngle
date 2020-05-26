package com.example.eulerangle;

import android.annotation.SuppressLint;
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

import com.kircherelectronics.fsensor.BaseFilter;
import com.kircherelectronics.fsensor.filter.averaging.MeanFilter;
import com.kircherelectronics.fsensor.linearacceleration.LinearAcceleration;
import com.kircherelectronics.fsensor.linearacceleration.LinearAccelerationFusion;
import com.kircherelectronics.fsensor.observer.SensorSubject;
import com.kircherelectronics.fsensor.sensor.FSensor;
import com.kircherelectronics.fsensor.sensor.acceleration.AccelerationSensor;
import com.kircherelectronics.fsensor.sensor.acceleration.ComplementaryLinearAccelerationSensor;
import com.kircherelectronics.fsensor.sensor.acceleration.KalmanLinearAccelerationSensor;
import com.kircherelectronics.fsensor.sensor.acceleration.LinearAccelerationSensor;
import com.kircherelectronics.fsensor.sensor.gyroscope.ComplementaryGyroscopeSensor;
import com.kircherelectronics.fsensor.sensor.gyroscope.GyroscopeSensor;
import com.kircherelectronics.fsensor.sensor.gyroscope.KalmanGyroscopeSensor;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends Activity {

    private static final String TAG = "sensor";
    double[] gravity = new double[3];
    double[] magneticFieldValues = new double[3];
    double[] accelerometerValues = new double[3];
    double[] gravity_pre = new double[3];
    double[] rotationvector = new double[4];
    float[] t_a = new float[3];
    double[] oula = new double[3];
    float[] l_a = new float[3];
    double ng_a[] = new double[3];
    double[] t_g = new double[3];

    private SensorManager sm;
    //需要两个Sensor
    private Sensor aSensor;
    private Sensor mSensor;
    private Sensor lSensor;
    private Sensor rSensor;
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
    long a_t;
    long g_t;
    boolean f_y = true;
    double y_begin;
    FSensor linearaccSensor;
    FSensor gyroscopeSensor;
    BaseFilter baseFilter;
    //final Context context;
    DecimalFormat decimalFormat = new DecimalFormat("0.0000");
    Queue<Double> queue = new LinkedList<>();
    FileOutputStream fileOutputStream = null;
    BufferedWriter bufferedWriter = null;
    int r_count = 0;

    private SensorSubject.SensorObserver gyroscopeObserver = new SensorSubject.SensorObserver() {
        @Override
        public void onSensorChanged(float[] values) {
            for(int i = 0; i < 4; i++) {
                rotationvector[i] = Double.parseDouble(String.valueOf(values[i]));
            }
        }
    };

    private SensorSubject.SensorObserver linearaccObserver = new SensorSubject.SensorObserver() {
        @Override
        public void onSensorChanged(float[] values) {
            end = System.currentTimeMillis();
            //float[] filteredAcceleration = ((MeanFilter) baseFilter).filter(values);
            for(int i = 0; i < 3; i++) {
                accelerometerValues[i] = Double.parseDouble(String.valueOf(values[i]));
                //Log.d(TAG, "asdasdasd:" + accelerometerValues[i] + " " + filteredAcceleration[i]);
            }
            Log.d(TAG, "vu: " + values[0] + " " + values[1] + " " + values[2]);
            calculateOrientation();
            start = end;
        }
    };
    final SensorEventListener myListener1 = new SensorEventListener() {
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY){
                    g_t = System.currentTimeMillis();
                    float[] aa = sensorEvent.values;
                    for(int i = 0; i < 3; i++) {
                        gravity[i] = Double.parseDouble(String.valueOf(aa[i]));
                        Log.d(TAG, "asdasdasd:" + gravity[i] + " " + aa[i]);
                    }
                }

            }



        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    final SensorEventListener myListener2 = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            end = System.currentTimeMillis();
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                l_a = sensorEvent.values;
                if(f_y && rotationvector[0] != 0){
                    oula[0] = 0;
                    y_begin = Math.toDegrees(rotationvector[0]);
                    f_y = false;
                    Log.d(TAG, "y_beginasdasd: " + rotationvector[0]);
                }else {
                    oula[0] = Math.toDegrees(rotationvector[0]) - y_begin;
                    //Log.d(TAG, "y_begin: " + y_begin);
                    Log.d(TAG, "oula[0]: " + oula[0]);
                }
                //oula[0] = Math.toDegrees(rotationvector[0]);
        /*oula2[1] = (float)Math.toDegrees(oula2[1]);
        oula2[2] = (float)Math.toDegrees(oula2[2]);*/
                oula[1] = Math.toDegrees(rotationvector[1]);
                oula[2] = Math.toDegrees(rotationvector[2]);
                double a = oula[1]*Math.PI/180;
                double b = oula[2]*Math.PI/180;
                double c = oula[0]*Math.PI/180;


                double G = Math.sqrt(gravity[0]*gravity[0] + gravity[1]*gravity[1] + gravity[2]*gravity[2]);
                //double G = 9.88;
        /*t_g[0] = (-Math.cos(a)*Math.sin(b))*G;
        t_g[1] = -Math.sin(a)*G;
        t_g[2] = Math.cos(a)*Math.cos(b)*G;*/

                t_g[0] = Math.cos(a)*Math.sin(b)*G;
                t_g[1] = -Math.sin(a)*G;
                t_g[2] = Math.cos(a)*Math.cos(b)*G;

                ng_a[0] = (float) ((double)l_a[0]-t_g[0]);
                ng_a[1] = (float) ((double)l_a[1]-t_g[1]);
                ng_a[2] = (float) ((double)l_a[2]-t_g[2]);
                t_a[0] = (float) ((Math.cos(b)*Math.cos(c) + Math.sin(a)*Math.sin(b)*Math.sin(c))*ng_a[0] + Math.cos(a)*Math.sin(c)*ng_a[1] + (Math.cos(b)*Math.sin(a)*Math.sin(c) - Math.cos(c)*Math.sin(b))*ng_a[2]);
                t_a[1] = (float) ((Math.cos(c)*Math.sin(a)*Math.sin(b) - Math.cos(b)*Math.sin(c))*ng_a[0] + Math.cos(a)*Math.cos(c)*ng_a[1] + (Math.sin(b)*Math.sin(c) + Math.cos(b)*Math.cos(c)*Math.sin(a))*ng_a[2]);
                t_a[2] = (float) (Math.cos(a)*Math.sin(b)*ng_a[0] + (-Math.sin(a))*ng_a[1] + Math.cos(a)*Math.cos(b)*ng_a[2]);
                t_a = ((MeanFilter) baseFilter).filter(t_a);
                //double acc = Math.sqrt(t_a[0]*t_a[0] + t_a[1]*t_a[1] + t_a[2]*t_a[2]);
        /*double a = oula[1]*Math.PI/180;
        double b = oula[2]*Math.PI/180;*/
                calculateOrientation();

            }
            start = end;

        }



        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    final SensorEventListener myListener3 = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                float[] aa = sensorEvent.values;
                for (int i = 0; i < 3; i++) {
                    magneticFieldValues[i] = Double.parseDouble(String.valueOf(aa[i]));
                    Log.d(TAG, "asdasdasd:" + magneticFieldValues[i] + " " + aa[i]);
                }
            }
        }


        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    /*final SensorEventListener myListener4 = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] aa = sensorEvent.values;
                for (int i = 0; i < 5; i++) {
                    rotationvector[i] = Double.valueOf(String.valueOf(aa[i]));
                    Log.d(TAG, "rotation:" + rotationvector[i] + " " + aa.length);
                }
            }
        }


        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };*/

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
        aSensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        //mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //lSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //rSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sm.registerListener(myListener1, aSensor, 0);
        //sm.registerListener(myListener3, mSensor, 0);
        //sm.registerListener(myListener2, lSensor, 0);
        //sm.registerListener(myListener4, rSensor, 0);
        baseFilter = new MeanFilter();
        linearaccSensor = new AccelerationSensor(this);
        linearaccSensor.register(linearaccObserver);
        linearaccSensor.start();
        gyroscopeSensor = new GyroscopeSensor(this);
        gyroscopeSensor.register(gyroscopeObserver);
        gyroscopeSensor.start();
        ((MeanFilter) baseFilter).setTimeConstant((float) 0.5);
        //更新显示数据的方法
        //if (debug == 1) {

        //}

        calculateOrientation();
        start = System.currentTimeMillis();
        Button button1 = findViewById(R.id.button);
        button1.setOnClickListener(v->{
            if(debug == 1) {
                debug = 0;
            }else{
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
                    Date date = new Date();
                    fileOutputStream = openFileOutput(sdf.format(date) + ".csv", Context.MODE_PRIVATE);
                    bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
                    bufferedWriter.write("delta_t,x,y,z\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        sm.unregisterListener(myListener1);
        super.onPause();
        if(debug == 1) {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void calculateOrientation() {
        double[] l_a = accelerometerValues;
        //double[] g_a = gravity;
        double alpha = 0.9;
        /*gravity_pre[0] = alpha * gravity_pre[0] + (1 - alpha) * g_a[0];
        gravity_pre[1] = alpha * gravity_pre[1] + (1 - alpha) * g_a[1];
        gravity_pre[2] = alpha * gravity_pre[2] + (1 - alpha) * g_a[2];
        l_a[0] = l_a[0]-gravity_pre[0];
        l_a[1] = l_a[1]-gravity_pre[1];
        l_a[2] = l_a[2]-gravity_pre[2];*/
        /*double t_a = Math.sqrt(l_a[0]*l_a[0]+l_a[1]*l_a[1]+l_a[2]*l_a[2]);
        if(l_a[1] < 0){
            t_a = -t_a;
        }*/
        //double[] oula = new double[3];
        /*float[] oula2 = new float[3];
        float[] R = new float[9];
        float[] g_fl = {(float)gravity[0], (float)gravity[1], (float)gravity[2]};
        //float[] g_fl = {(float)accelerometerValues[0], (float)accelerometerValues[1], (float)accelerometerValues[2]};
        float[] m_fl = {(float)magneticFieldValues[0], (float)magneticFieldValues[1], (float)magneticFieldValues[2]};
        SensorManager.getRotationMatrix(R,null, g_fl,  m_fl);
        SensorManager.getOrientation(R, oula2);
        Log.d(TAG, "accelerometerValues: " + R[0] + " " + R[1] + " " + R[2]);*/

        /*float[] rv = {(float)rotationvector[0], (float)rotationvector[1], (float)rotationvector[2]};
        SensorManager.getRotationMatrixFromVector(R, rv);
        SensorManager.getOrientation(R, oula2);*/

        // 要经过一次数据格式的转换，转换为度
        if(f_y && rotationvector[0] != 0){
            oula[0] = 0;
            y_begin = Math.toDegrees(rotationvector[0]);
            f_y = false;
            Log.d(TAG, "y_beginasdasd: " + rotationvector[0]);
        }else {
            oula[0] = Math.toDegrees(rotationvector[0]) - y_begin;
            //Log.d(TAG, "y_begin: " + y_begin);
            Log.d(TAG, "oula[0]: " + oula[0]);
        }
        //oula[0] = Math.toDegrees(rotationvector[0]);
        //*oula2[1] = (float)Math.toDegrees(oula2[1]);
        //oula2[2] = (float)Math.toDegrees(oula2[2]);
        oula[1] = Math.toDegrees(rotationvector[1]);
        oula[2] = Math.toDegrees(rotationvector[2]);
        double a = oula[1]*Math.PI/180;
        double b = oula[2]*Math.PI/180;
        double c = oula[0]*Math.PI/180;

        //double[] t_g = new double[3]
        //double acc = Math.sqrt(t_a[0]*t_a[0] + t_a[1]*t_a[1] + t_a[2]*t_a[2]);
        //*double a = oula[1]*Math.PI/180;
        //double b = oula[2]*Math.PI/180;*//*
        //double G = Math.sqrt(gravity[0]*gravity[0] + gravity[1]*gravity[1] + gravity[2]*gravity[2]);
        double G = 9.70;
        /*t_g[0] = (-Math.cos(a)*Math.sin(b))*G;
        t_g[1] = -Math.sin(a)*G;
        t_g[2] = Math.cos(a)*Math.cos(b)*G;*/

        t_g[0] = Math.cos(a)*Math.sin(b)*G;
        t_g[1] = -Math.sin(a)*G;
        t_g[2] = Math.cos(a)*Math.cos(b)*G;

        l_a[0] = l_a[0]-t_g[0];
        l_a[1] = l_a[1]-t_g[1];
        l_a[2] = l_a[2]-t_g[2];

        float[] t_a = new float[3];
        t_a[0] = (float)((Math.cos(b)*Math.cos(c) + Math.sin(a)*Math.sin(b)*Math.sin(c))*l_a[0] + Math.cos(a)*Math.sin(c)*l_a[1] + (Math.cos(b)*Math.sin(a)*Math.sin(c) - Math.cos(c)*Math.sin(b))*l_a[2]);
        t_a[1] = (float)((Math.cos(c)*Math.sin(a)*Math.sin(b) - Math.cos(b)*Math.sin(c))*l_a[0] + Math.cos(a)*Math.cos(c)*l_a[1] + (Math.sin(b)*Math.sin(c) + Math.cos(b)*Math.cos(c)*Math.sin(a))*l_a[2]);
        t_a[2] = (float)(Math.cos(a)*Math.sin(b)*l_a[0] + (-Math.sin(a))*l_a[1] + Math.cos(a)*Math.cos(b)*l_a[2]);
        t_a = ((MeanFilter) baseFilter).filter(t_a);
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
        if(debug == 1) {
            try {
                bufferedWriter.write((double)(end - start)/1000d + "," + t_a[0] + "," + t_a[1] + "," + t_a[2] + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                //textView1.setText("x: " + win_a[i]*100);
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
                //textView4.setText("vel: " + vel[i] * 100 + "cm/s\nv_s: " + decimalFormat.format(v_s));
                Log.d(TAG, "vel: " + vel[i]);
            }
            pos += v_pre*delta_t/1000d;
            /*for(int i = 1; i < win_size; i++){
                pos += vel[i]*(double)delta_t/1000d;
                Log.d(TAG, "qwe:" + o_a[i] + "," + x_arr[i] + "," + win_a[i] + "," + vel[i] + "," + pos + "\n");
                if(debug == 1) {
                    try {
                            bufferedWriter.write(o_a[i] + "," + x_arr[i] + "," + win_a[i] + "," + vel[i] + "," + pos + "," + (double)delta_time[i]/1000 + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }*/


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
        //textView6.setText("vvv: " + v*100 + "cm/s");

        textView2.setText("x:" + l_a[0]*100 + "\ny:" + l_a[1]*100 + "\nz:" + l_a[2]*100 + "\ng:" + Math.sqrt(l_a[0]*l_a[0]+l_a[1]*l_a[1]+l_a[2]*l_a[2]));
        textView7.setText("\nyaw: " + oula[0] + "\npitch: " + oula[1] + "\nroll: " + oula[2] + "\nt_x: " + t_a[0]*100 + "\nt_y: " + t_a[1]*100 + "\nt_z: " + t_a[2]*100 + "\nG: " + G);

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
