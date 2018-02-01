package com.ssong.api.myparking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;

/**
 * Created by ssong on 2017-09-08.
 */

public class SearchList extends Activity{
    private String titleStr;
    private String addrStr;
    private String SearchDate;

    ParkingMain beforeAct = (ParkingMain) ParkingMain.beforeAct;

    InputMethodManager imm; // 키보드 변수

    // 리스트 뷰 시작
    ListView listview;
    SearchAdapter adapter;
    private String result;
    Handler mHandler;

    private EditText editText;
    private int search_cnt=0;

    private static String clientId = "aLfmcKTf2PlxBt5mSMdz";//애플리케이션 클라이언트 아이디값";
    private static String clientSecret = "3Z87iyNno1";//애플리케이션 클라이언트 시크릿값";
    private TextView list_footer;

    public void setTitle(String title) {
        titleStr = title ;
    }
    public void setAddr(String addr) {
        addrStr = addr ;
    }
    public void setSearchDate(String date) {
        SearchDate = date ;
    }

    public String getTitleName() {
        return this.titleStr ;
    }
    public String getAddr() {
        return this.addrStr ;
    }
    public String getSearchDate() {
        return this.SearchDate ;
    }

    // DB 관련
    // 상수 관련
    static private String dbName = "search_db"; // name of Database;
    static private String tableName = "history"; // name of Table;
    static private int dbMode = Context.MODE_PRIVATE;

    // Database 관련 객체들
    SQLiteDatabase db;

    @Override
    public void onPause() {
        super.onPause();
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    // 화면 터치시 키보드 내리기
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();
        if (v != null &&
                (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) &&
                v instanceof EditText &&
                !v.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            v.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + v.getLeft() - scrcoords[0];
            float y = ev.getRawY() + v.getTop() - scrcoords[1];

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom())
                hideKeyboard(this);
        }
        return super.dispatchTouchEvent(ev);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_main);

        editText = (EditText) findViewById(R.id.search_box2);
        editText.requestFocus();

        // footer 등록(검색 히스토리 삭제)
        final View footer = getLayoutInflater().inflate(R.layout.search_footer, null, false) ;

        //키보드 보이게 하는 부분
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        // Adapter 생성
        adapter = new SearchAdapter() ;
        // 리스트뷰 참조 및 Adapter달기
        listview = (ListView) findViewById(R.id.listitem);

        // footer 추가
        DB_Search_Histroy();  // DB연결
        // 검색 히스토리가 존재하는지 체크
        String sql = "select * from " + tableName + " limit 1;";
        Cursor results = db.rawQuery(sql, null);
        // 존재하지 않으면 검색 도움말 추가
        if(results.getCount()==0){
            listview.addFooterView(footer);
            list_footer = (TextView) findViewById(R.id.list_footer);
            list_footer.setText("목적지나 지역명을 검색하세요");
        }
        // 존재하면 footer 추가
        else{
            // footer 추가
            listview.addFooterView(footer);
            list_footer = (TextView) findViewById(R.id.list_footer);

            // 검색 기록 삭제 리스너 추가
            list_footer.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeAllData();
                    adapter.listViewItemList.clear();
                    adapter.notifyDataSetChanged();
                    listview.removeFooterView(footer);
                }
            });
        }

        // 리스트 아이템 추가
        listview.setAdapter(adapter);
        mHandler = new Handler(Looper.getMainLooper());

        // 리스트 클릭했을때 이벤트 핸들러 정의.
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // get item
                final SearchList item = (SearchList) parent.getItemAtPosition(position) ;
                new Thread(){
                    @Override
                    public void  run(){
                        // DB 히스토리에 기록
                        Calendar cal = Calendar.getInstance();
                        insertData(item.getTitleName(),item.getAddr(), cal.get(Calendar.MONTH)+1+"/"+cal.get(Calendar.DATE)+" 검색");
                        // 주소 좌표 변환
                        Tran_LatLng(item.getAddr());
                    }
                }.start();
                String titleStr = item.getTitleName();
            }
        });


        // text 변화 리스너
        editText.addTextChangedListener(new TextWatcher() {
            final android.os.Handler handler = new android.os.Handler();
            Runnable runnable;
            // 입력되는 텍스트에 변화가 있을 때
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(runnable);

                // 입력된 문구가 없을 때
                if(s.length() == 0){
                    // 리스트 초기화
                    adapter.listViewItemList.clear();

                    String sql = "select id, title, addr, date from " + tableName + " order by id desc limit 5;";
                    Cursor results = db.rawQuery(sql, null);

                    results.moveToFirst();

                    while(!results.isAfterLast()){
                        adapter.addItem(results.getString(1),results.getString(2),results.getString(3));
                        results.moveToNext();
                    }
                    results.close();
                    adapter.notifyDataSetChanged();
                    // footer 추가
                    listview.addFooterView(footer);
                }
            }
            // 입력이 끝났을 때
            @Override
            public void afterTextChanged(Editable editable) {
                //show some progress, because you can access UI here
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        // Thread 실행
                        new Thread(){
                            @Override
                            public void  run(){
                                SearchMap(editText.getText().toString());
                            }
                        }.start();
                    }
                };
                handler.postDelayed(runnable, 350);
                // footer 제거
                if(editable.length() != 0) {
                    listview.removeFooterView(footer);
                }
            }
            // 입력하기 전에
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });

    }

/*

// 핸들러 실행
Handler mHandler = new Handler(Looper.getMainLooper());
mHandler.postDelayed(new Runnable() {
    @Override
    public void run() {
         // 내용
    }
}, 0);

// 스레드 실행
new Thread(){
    @Override
    public void  run(){
        SearchMap(editText.getText().toString());
    }
}.start();
*/
    // 키보드 내리기
    public void hideKeyboard(Activity activity) {
        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    // 검색 API
    public void SearchMap(String keyword){
        try {
            String text = URLEncoder.encode(keyword, "UTF-8");
            String apiURL = "https://openapi.naver.com/v1/search/local?display=20&query="+ text; // json 결과
            //String apiURL = "https://openapi.naver.com/v1/search/blog.xml?query="+ text; // xml 결과
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            int responseCode = con.getResponseCode();

            BufferedReader br;
            if(responseCode==200) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            result = response.toString();
            // 태그 제거
            result = result.replaceAll("<b>","");
            result = result.replaceAll("</b>","");

            // 스레드안이라 핸들러 처리가 필요
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    JSON_Mark(result);
                }
            }, 1000);

            // 검색을 3회 했을시 DB에 전달
            if(search_cnt>2){
                // 검색 정보 DB에 전달
                String test = "https://www.namanapp.co.kr/php/api/parking/php/search_action.php?userid=" +getCurrentUserId()+ "&cnt="+search_cnt;
                DBConnect task = new DBConnect(test);
                task.start();

                search_cnt=0;
            }
            else{
                search_cnt=search_cnt+1;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // JSON 데이터를 파싱합니다.
    // URLConnector로부터 받은 String이 JSON 문자열이기 때문입니다.
    private void JSON_Mark(String result) {
        try {
            JSONObject json = new JSONObject(result);
            // Log.d("NMapSearch", "ssong: " + result);
            JSONArray arr = json.getJSONArray("items");

            // 리스트 초기화
            adapter.listViewItemList.clear();
            adapter.notifyDataSetChanged();

            for(int i = 0; i < arr.length(); i++){
                JSONObject json2 = arr.getJSONObject(i);
            //    Log.d("NMapSearch", "ssong: " + json2.getString("title"));
                //      Tran_LatLng(json2.getString("address"));
                adapter.addItem(json2.getString("title"), json2.getString("address"),"");
            }
            adapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 주소를 위도 경도 변환
    private void Tran_LatLng(String text){
        try {
            text = URLEncoder.encode(text, "UTF-8");
            String apiURL = "https://openapi.naver.com/v1/map/geocode?encoding=utf-8&coordType=latlng&query=" + text; // json 결과

            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode==200) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            String result;
            result = response.toString();
            Log.d("NMapSearch", "ssong: " + result);
            //JSONObject json_parse = json_data.getJSONObject("GetParkInfo");
            //JSONArray arr = json_parse.getJSONArray("row");

            JSONObject json = new JSONObject(result);
            JSONArray arr = json.getJSONObject("result").getJSONArray("items");
            JSONObject json2 = arr.getJSONObject(0).getJSONObject("point");

            Log.d("NMapSearch", "ssong: " + String.valueOf(json2.getDouble("x")));
            Log.d("NMapSearch", "ssong: " + String.valueOf(json2.getDouble("y")));
      //      JSONArray arr = json.getJSONArray("items");
            Intent intent = new Intent(SearchList.this, ParkingMain.class);
            intent.putExtra("lat",json2.getDouble("x"));
            intent.putExtra("loc",json2.getDouble("y"));
            beforeAct.finish();
            startActivity(intent);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Database 생성 및 열기
    public void createDatabase(String dbName, int dbMode){
        db = openOrCreateDatabase(dbName,dbMode,null);
    }

    // Table 생성
    public void createTable(){
        String sql = "create table if not exists " + tableName + "(id integer primary key autoincrement, "+"title text not null, "+"addr text not null, "+"date text not null)";
        db.execSQL(sql);
    }


    // Table 삭제
    public void removeTable(){
        String sql = "drop table " + tableName;
        db.execSQL(sql);
    }


    // Data 추가
    public void insertData(String title, String addr, String date){
        String sql = "insert into " + tableName + " values(NULL, '" + title +"', '" + addr +"', '" + date +"');";
        db.execSQL(sql);
    }

    // Data 업데이트
    public void updateData(int index, String title){
        String sql = "update " + tableName + " set title = '" + title +"' where id = "+index +";";
        db.execSQL(sql);
    }

    // Data 삭제
    public void removeData(){
        String sql = "delete from " + tableName +
                " where id not in (select max(id) as id from " + tableName + " group by title, addr order by id desc limit 5)";

        db.execSQL(sql);
    }

    // 모든 Data 삭제
    public void removeAllData(){
        String sql = "delete from " + tableName + ";";
        db.execSQL(sql);
    }

    // Data 읽기(꺼내오기)
    public void selectData(int index){
        String sql = "select * from " +tableName+ " where id = "+index+";";
        Cursor result = db.rawQuery(sql, null);

        // result(Cursor 객체)가 비어 있으면 false 리턴
        if(result.moveToFirst()){
            int id = result.getInt(0);
            String title = result.getString(1);
        }
        result.close();
    }


    // 모든 Data 읽기
    public void selectAll(){
        String sql = "select * from " + tableName + ";";
        Cursor results = db.rawQuery(sql, null);

        results.moveToFirst();

        while(!results.isAfterLast()){
            int id = results.getInt(0);
            String title = results.getString(1);
            Log.d("search", "ssong: " + title);
            results.moveToNext();
        }
        results.close();
    }

    // 최초 DB 연결 후 데이터 가져오기
    public void DB_Search_Histroy(){
        createDatabase(dbName,dbMode);
        createTable();

        // 최초 검색창 클릭했을때 한번 실행
        adapter.listViewItemList.clear();

        removeData(); // 10건 남기고 삭제한다.
        String sql = "select id, title, addr, date from " + tableName + " order by id desc limit 5;";
        Cursor results = db.rawQuery(sql, null);

        results.moveToFirst();

        while(!results.isAfterLast()){
            adapter.addItem(results.getString(1),results.getString(2),results.getString(3));
            results.moveToNext();
        }
        results.close();
        adapter.notifyDataSetChanged();

    }

    // 유저 디바이스 아이디 가져오기
    public String getCurrentUserId(){
        String deviceId="";
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return deviceId;
    }
}
