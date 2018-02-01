package com.ssong.api.myparking;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.kakao.kakaonavi.KakaoNaviParams;
import com.kakao.kakaonavi.KakaoNaviService;
import com.kakao.kakaonavi.Location;
import com.kakao.kakaonavi.NaviOptions;
import com.kakao.kakaonavi.options.CoordType;
import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapCompassManager;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapLocationManager;
import com.nhn.android.maps.NMapOverlay;
import com.nhn.android.maps.NMapOverlayItem;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.mapviewer.overlay.NMapMyLocationOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;
import com.skp.Tmap.TMapTapi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ParkingMain extends NMapActivity {
    public static Activity beforeAct;
    private static final String LOG_TAG = "ParkingMain";
    private static final boolean DEBUG = false;

    private NMapView mMapView;// 지도 화면 View
    private final String CLIENT_ID = "aLfmcKTf2PlxBt5mSMdz";// 애플리케이션 클라이언트 아이디 값

    private MapContainerView mMapContainerView;
    private NMapController mMapController;
    private NMapLocationManager mMapLocationManager;
    private NMapMyLocationOverlay mMyLocationOverlay;
    private NMapOverlayManager mOverlayManager;
    private NMapCompassManager mMapCompassManager;
    private NMapViewerResourceProvider mMapViewerResourceProvider;
    private static boolean mIsMapEnlared = false;
    private static boolean USE_XML_LAYOUT = true;
    private Dialog dialog;
    private double content_height;
    private DisplayMetrics dm;

    private Bundle bd;
    private double search_lat, search_loc, before_search_lat, before_search_loc, here_lat, here_loc;
    private float navi_lat, navi_loc;
    private String pay_default, pay_add, pay_day, pay_month, free_sat, free_holi, pay_one_hour, pay_two_hour, pay_six_hour;
    private boolean search_flag=false;
    private ImageButton btn_user_loc, btn_user_move, btn_map_sizeplus, btn_map_sizeminus, btn_content_pay, btn_content_navi
            , btn_content_report, btn_user_parking_add, btn_user_add,  btn_tutorial;
    private TextView content_title, content_addr, content_tel, content_time, content_distance, content_code;
    private ImageView park_guide_view;
    private LinearLayout content;
    private SharedPreferences prefs;

    Animation slideUp;
    Animation slideDown;
    private String AppVersion;

    private boolean is_move=false; // 길안내 모드에서는 GPS STOP이 되지 않는다.

    EditText search_box;

    // DB 관련
    // 상수 관련
    static private String dbName = "search_db"; // name of Database;
    static private String tableName = "parking_info"; // name of Table;
    static private int dbMode = Context.MODE_PRIVATE;
    private static int MAX_USER_CALL_DB = 200;

    // Database 관련 객체들
    SQLiteDatabase db;

    @Override
    public void onPause() {
        super.onPause();
        stopMyLocation();
        Thread.interrupted();  // Thread 강제 종료
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMyLocation();
        Thread.interrupted();  // Thread 강제 종료
    }

    @Override
    public void onBackPressed() {
        // 세부창이 열려있으면 세부창을 먼저 닫음
        if(content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.GONE); // or GONE
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(ParkingMain.this,android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
            builder.setTitle("나만의주차장")
                    .setMessage("앱을 종료하시겠습니까?")
                    .setCancelable(true)

                    .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                            // System.exit(0);
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //finish(); 강제 종료 할지 말지 여부
                        }
                    })
                    .setCancelable(false) // 확인 취소 외에 다른곳 터치 비활성화
                    .create()
                    .show();
        }
    }

    @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 이전 액티비티 변수
        beforeAct = ParkingMain.this;

        if (USE_XML_LAYOUT) {
            setContentView(R.layout.parking_main);
            mMapView = (NMapView) findViewById(R.id.mapView);

            // 원래 없는데 나침반 모드때문에 추가
            mMapContainerView = new MapContainerView(this);
        } else {
            // create map view
            mMapView = new NMapView(this);

            // create parent view to rotate map view
            mMapContainerView = new MapContainerView(this);
            mMapContainerView.addView(mMapView);

            // set the activity content to the parent view
            setContentView(mMapContainerView);
        }
        mMapView.setClientId(CLIENT_ID); // 클라이언트 아이디 값 설정

        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        mMapView.setFocusable(true);
        mMapView.setFocusableInTouchMode(true);
        mMapView.requestFocus();

        //  현재 위치 메소드 등록
        mMapLocationManager = new NMapLocationManager(this);
        mMapLocationManager.setOnLocationChangeListener(onMyLocationChangeListener);

        // create resource provider
        mMapViewerResourceProvider = new NMapViewerResourceProvider(this);

        // use map controller to zoom in/out, pan and set map center, zoom level etc.
        mMapController = mMapView.getMapController();

        // compass manager
        mMapCompassManager = new NMapCompassManager(this);

        // create overlay manager
        mOverlayManager = new NMapOverlayManager(this, mMapView, mMapViewerResourceProvider);

        // 유저의 현재 위치를 가져옴
        mMyLocationOverlay = mOverlayManager.createMyLocationOverlay(mMapLocationManager, mMapCompassManager);

        // 맵 관련 리스너 등록
        // register listener for map state changes
        mMapView.setOnMapStateChangeListener(onMapViewStateChangeListener);
        // 클릭 관련 리스너
        mMapView.setOnMapViewTouchEventListener(onMapViewTouchEventListener);

        // 말풍선 관련 리스너
        mOverlayManager.setOnCalloutOverlayViewListener(onCalloutOverlayViewListener);

        // 유저의 현재 위치 버튼 등록
        // findViewById(R.id.user_loc).setOnClickListener(mClickListener);
        // 버튼 등록
        btn_user_loc = (ImageButton) findViewById(R.id.user_loc);
        btn_user_move = (ImageButton) findViewById(R.id.user_move);
        btn_content_pay = (ImageButton) findViewById(R.id.content_pay);
        btn_content_navi = (ImageButton) findViewById(R.id.content_navi);
        btn_tutorial = (ImageButton) findViewById(R.id.user_parking_guide);
        btn_content_report = (ImageButton) findViewById(R.id.content_report);
        btn_map_sizeplus = (ImageButton) findViewById(R.id.map_sizeplus);
        btn_map_sizeminus = (ImageButton) findViewById(R.id.map_sizeminus);
        btn_user_parking_add = (ImageButton) findViewById(R.id.user_parking_add);
        btn_user_add = (ImageButton) findViewById(R.id.user_add);

        content = (LinearLayout) findViewById(R.id.content);
        content_title = (TextView) findViewById(R.id.content_title);
        content_addr = (TextView) findViewById(R.id.content_addr);
        content_tel = (TextView) findViewById(R.id.content_tel);
        content_time = (TextView) findViewById(R.id.content_time);
        content_distance = (TextView) findViewById(R.id.content_distance);
        content_code = (TextView) findViewById(R.id.content_code);
        park_guide_view = (ImageView) findViewById(R.id.park_guide_view);

        // 최초 실행 변수
        prefs = getSharedPreferences("Pref", MODE_PRIVATE);

        // 로그인 sp호출
        AppVersion = getResources().getString(R.string.check_version);
        String test = "https://www.namanapp.co.kr/php/api/parking/php/login.php?userid=" +getCurrentUserId()+"&appver="+AppVersion;
        DBConnect task = new DBConnect(test);
        task.start();
        try{
            task.join();
        }
        catch(InterruptedException e){
        }
        String result = task.getResult();
        if(result.trim().equals("0")){
            AlertDialog.Builder builder = new AlertDialog.Builder(ParkingMain.this);
            builder.setTitle("일별 검색수 초과");
            builder.setMessage("죄송합니다.\n네이버 일별 검색수를 초과하여 오늘은 더 이상 사용할 수 없습니다. \n내일 다시 사용 부탁드립니다.");
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

            AlertDialog diag = builder.create();
            diag.setCancelable(false); // 확인 취소 외에 다른곳 터치 비활성화
            diag.show();
        }

        // 유저 위치 버튼 등록
        btn_user_loc.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMyLocation();
                // 깜박이는 애니메이션
                Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
                btn_user_loc.startAnimation(startAnimation);
            }
        });
        // 유저 이동 버튼 등록
        btn_user_move.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                MoveMyLocation();
                // 추가 버튼 닫음
                btn_user_add.setVisibility(View.GONE);
                // 하단 창 닫기
                content.setVisibility(View.GONE);
            }
        });
        // 유저 맵 확장 버튼
        btn_map_sizeplus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapController.zoomIn();
            }
        });
        // 유저  버튼
        btn_map_sizeminus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapController.zoomOut();
            }
        });
        // 주차장 추가 제보 버튼
        btn_user_parking_add.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btn_user_add.getVisibility()==View.GONE) {
                    btn_user_add.setVisibility(View.VISIBLE);
                    Toast.makeText(ParkingMain.this, "추가할 지역을 가운데 원에 넣고 원을 터치하세요", Toast.LENGTH_LONG).show();
                    content.setVisibility(View.GONE);
                    // 깜박이는 애니메이션
                    Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
                    btn_user_parking_add.startAnimation(startAnimation);
                }
                else
                    btn_user_add.setVisibility(View.GONE);
            }
        });

        // 주차장 추가 제보 버튼
        btn_user_add.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                parking_add();

                btn_user_add.setVisibility(View.GONE);
            }
        });


        // 제보하기 버튼
        btn_content_report.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Report();
            }
        });

        // 네비게이션 이동 버튼 등록
        btn_content_navi.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(navi_lat > 0 && navi_loc > 0) // 값이 있을때만 네비앱으로 이동
                {
                    MoveNavi();
                }
            }
        });

        // 도움말 버튼 등록
        btn_tutorial.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                tutorial();
            }
        });

        // 요금정보 이동 버튼 등록
        btn_content_pay.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                MovePay();
            }
        });

        // 인텐트 전달 받은 값 실행 (리스트 클릭 좌표로 이동)
        if(savedInstanceState == null)
        {
            // DB 및 테이블 생성
            createDatabase(dbName,dbMode);
            bd = getIntent().getExtras();
            // 처음으로 켯을때
            if(bd == null){
                // removeTable();
                removeAllData(); // 데이터 전체 삭제(테이블 없을 시 스킵)
                createTable();   // 테이블 생성

                // DB에서 최초 데이터 가져오기
                DB_ParkingInfo();

                // DB에서 최초 데이터 가져오기
                int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
                if(permissionCheck== PackageManager.PERMISSION_DENIED){
                    // 권한 없을시(기본 위치 좌표로 이동)
                    MoveArea(126.97894231, 37.5629588);
                }else{
                    // 권한 있을시 현재 위치 탐색
                    startMyLocation();
                }
                // 지도 중앙을 중심으로 마커 가져오기
                select_marker(mMapController.getMapCenter().getLatitude(),mMapController.getMapCenter().getLongitude());
            }
            // 검색으로 돌아왔을 때
            else{
                // 최초로 권한을 획득하였을 때
                if(bd.getBoolean("permission") == true){
                    startMyLocation();
                }
                else {
                    // 같은 지명으로 2번이상 검색시에도 현재 지도에 마커 표시
                    select_marker(mMapController.getMapCenter().getLatitude(), mMapController.getMapCenter().getLongitude());
                    // 좌표 이동
                    MoveArea(bd.getDouble("lat"), bd.getDouble("loc"));
                    // 검색 플래그 변경(가운데 마커 표시용)
                    search_flag = true;
                    search_lat = bd.getDouble("lat");
                    search_loc = bd.getDouble("loc");
                }
            }
        }

        // 시작 축소 레벨
        mMapController.setZoomLevel(12);

        // 해상도별 지도 배율 설정
        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        switch (densityDpi)
        {
            case DisplayMetrics.DENSITY_LOW:
                // LDPI
            case DisplayMetrics.DENSITY_MEDIUM:
                // MDPI
                mMapView.setScalingFactor(1.0F, false);
                break;

            case DisplayMetrics.DENSITY_TV:
            case DisplayMetrics.DENSITY_HIGH:
                // HDPI
                mMapView.setScalingFactor(1.7F, true);
                break;

            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_280:
                // XHDPI
                mMapView.setScalingFactor(2.6F, true);
                break;

            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_360:
            case DisplayMetrics.DENSITY_400:
            case DisplayMetrics.DENSITY_420:
                // XXHDPI
                mMapView.setScalingFactor(4.0F, true);
                break;

            case DisplayMetrics.DENSITY_XXXHIGH:
            case DisplayMetrics.DENSITY_560:
                // XXXHDPI
                mMapView.setScalingFactor(5.0F, true);
                break;

            default:
                mMapView.setScalingFactor(5.0F, true);
                break;
        }

        // 검색창 아이디 가져오기
        search_box = (EditText) findViewById(R.id.search_box);
        // 검색창 이벤트
        search_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParkingMain.this, SearchList.class);
                startActivity(intent);
            }
        });

        // 하단 레이아웃 클릭시 뒤로 전달안되게
        content.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true; // false면 전달
            }
        });

        // 애니메이션 등록
        slideUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);

        // Log.d("ssong:", "Unable to get MessageDigest. signature=" + getKeyHash(ParkingMain.this));

        // content 창 높이 변경
        dm = getApplicationContext().getResources().getDisplayMetrics();
        // 소프트키 여부에 따라 높이가 달라 조정
        if(dm.heightPixels>2500){
            content_height = dm.heightPixels * 0.255;
        }
        else{
            content_height = dm.heightPixels * 0.28;
        }
        content.getLayoutParams().height=(int)Math.round(content_height);
    }

    // 앱 최초 설치시 한번만 동작
    public void checkFirstRun(){
        boolean isFirstRun = prefs.getBoolean("isFirstRun",true);
        if(isFirstRun)
        {
            prefs.edit().putBoolean("isFirstRun",false).apply();
            tutorial(); // 튜토리얼 화면 노출
        }
    }
    // 이동 모드로 변경
    private void MoveMyLocation() {
        if (mMyLocationOverlay != null) {
            // 확대 레벨 변경
            mMapController.setZoomLevel(12);

            if (!mOverlayManager.hasOverlay(mMyLocationOverlay)) {
                mOverlayManager.addOverlay(mMyLocationOverlay);
            }

            if (!mMapView.isAutoRotateEnabled()) {
                // 깜박이는 애니메이션
                Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
                btn_user_move.startAnimation(startAnimation);

                is_move = true;
                startMyLocation_move();
                mMyLocationOverlay.setCompassHeadingVisible(true);
                mMapCompassManager.enableCompass();
                mMapView.setAutoRotateEnabled(true, true);
                mMapContainerView.requestLayout();
                // 화면이 계속 켜짐상태로 있게 한다
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // 현재 위치 권한이 있는지 확인
                int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (mMyLocationOverlay != null && permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    // 버튼 눌림 효과
                    GlideDrawableImageViewTarget gifImage = new GlideDrawableImageViewTarget(btn_user_move);
                    Glide.with(this).load(R.drawable.navi_rotate).into(gifImage);
                }
                /* 애니메이션
                Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
                btn_user_move.startAnimation(startAnimation);
                */
            } else {
                stopMyLocation();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                is_move = false;

                // 버튼 눌림 효과
                Glide.clear(btn_user_move);
                btn_user_move.setImageResource(R.drawable.navi);
                /* 애니메이션 정지
                btn_user_move.clearAnimation();
                */
            }
            mMapView.postInvalidate();
        }
    }

    // 유저의 현재위치를 가져오기를 시작한다.
    public void startMyLocation() {
        boolean isGrantGPS = true;

        if (mMyLocationOverlay != null) {
            if (!mOverlayManager.hasOverlay(mMyLocationOverlay)) {
                mOverlayManager.addOverlay(mMyLocationOverlay);
            }
            // 권한 요청
            if (Build.VERSION.SDK_INT >= 23) {
                isGrantGPS = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (!isGrantGPS) {
                    Intent intent = new Intent(this, PermissionActivity.class);
                    startActivity(intent);
                }
            }
            if (isGrantGPS || Build.VERSION.SDK_INT < 23) {
                // 현재 위치로 이동
                boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(false);
                // GPS가 꺼져있으면
                if (!isMyLocationEnabled) {
                    Toast.makeText(ParkingMain.this, "GPS가 꺼져있습니다.", Toast.LENGTH_LONG).show();
                    Intent goToSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(goToSettings);
                    return;
                } else if (isMyLocationEnabled) {
                    // Thread 실행
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(4000);
                                if (is_move == false)
                                    stopMyLocation();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        }
    }

    // 이동 모드에서 현재위치 가져오기(꺼지지않음)
    private void startMyLocation_move() {
        boolean isGrantGPS = true;

        if (mMyLocationOverlay != null) {
            if (!mOverlayManager.hasOverlay(mMyLocationOverlay)) {
                mOverlayManager.addOverlay(mMyLocationOverlay);
            }
            // 권한 요청
            if (Build.VERSION.SDK_INT >= 23) {
                isGrantGPS = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (!isGrantGPS) {
                    Intent intent = new Intent(this, PermissionActivity.class);
                    startActivity(intent);
                }
            }
            if (isGrantGPS || Build.VERSION.SDK_INT < 23) {
                // 현재 위치로 이동
                boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(false);
                // GPS가 꺼져있으면
                if (!isMyLocationEnabled){
                    Toast.makeText(ParkingMain.this, "GPS가 꺼져있습니다.", Toast.LENGTH_LONG).show();
                    Intent goToSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(goToSettings);
                    return;
                }
            }
        }
    }

    // GPS를 멈춘다
    private void stopMyLocation() {
        if (mMyLocationOverlay != null) {
            mMapLocationManager.disableMyLocation();
            if (mMapView.isAutoRotateEnabled()) {
                mMyLocationOverlay.setCompassHeadingVisible(false);
                mMapCompassManager.disableCompass();
                mMapView.setAutoRotateEnabled(false, false);
                mMapContainerView.requestLayout();
            }
        }
    }

    // 지도 이동
    public void MoveArea(double lot, double lat) {
        NGeoPoint np = new NGeoPoint();
        np.set(lot, lat);
        mMapController.animateTo(np);
    }

    public void MovePay() {
        // 다이얼로그 리스트 뷰
        dialog = new Dialog(ParkingMain.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.pay_info_dialog);

        // 요금정보 창 사이즈 조절
        LinearLayout pay_main_view;
        pay_main_view = (LinearLayout) dialog.findViewById(R.id.pay_main_view);
        pay_main_view.getLayoutParams().height=(int)Math.round(dm.heightPixels * 0.58);
        pay_main_view.getLayoutParams().width=(int)Math.round(dm.widthPixels* 0.85);

        // 닫기 버튼
        ImageView btn_content_pay_close = (ImageView) dialog.findViewById(R.id.content_pay_close);

        dialog.show();

        // 요금 정보
        TextView default_pay = (TextView) dialog.findViewById(R.id.pay_dg_text1);
        TextView add_pay = (TextView) dialog.findViewById(R.id.pay_dg_text2);
        TextView free_yn = (TextView) dialog.findViewById(R.id.pay_dg_text3);
        TextView add_hour_pay = (TextView) dialog.findViewById(R.id.add_hour_pay);

        default_pay.setText(pay_default + "\n" + pay_add);

        // 모든 시간 요금 정보가 없으면 하나로 표시
        if(pay_one_hour.equals("1시간요금 : 정보없음") && pay_two_hour.equals("2시간요금 : 정보없음") && pay_six_hour.equals("6시간요금 : 정보없음")) {
            add_hour_pay.setText("시간요금 : 정보없음");
        }
        else{
            add_hour_pay.setText(pay_one_hour + "\n" + pay_two_hour + "\n" + pay_six_hour);
        }
        add_pay.setText(pay_day + "\n" + pay_month);
        free_yn.setText(free_sat + "\n" + free_holi);

        // 창 닫기
        btn_content_pay_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
    }
    // 제보하기
    public void Report(){
        // 다이얼로그 리스트 뷰
        dialog = new Dialog(ParkingMain.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.report_edit_dialog);
        dialog.setCanceledOnTouchOutside(false);

        // 제보하기 입력창 사이즈 조절
        LinearLayout report_edit_dialog;
        report_edit_dialog = (LinearLayout) dialog.findViewById(R.id.report_main);
        report_edit_dialog.getLayoutParams().height=(int)Math.round(dm.heightPixels * 0.5);
        report_edit_dialog.getLayoutParams().width=(int)Math.round(dm.widthPixels* 0.85);

        dialog.show();

        // 입력 창 버튼
        final EditText content_report_memo = (EditText) dialog.findViewById(R.id.content_report_memo);
        final TextView content_report_confirm = (TextView) dialog.findViewById(R.id.content_report_confirm);

        // 닫기 버튼
        final ImageView btn_content_report_close = (ImageView) dialog.findViewById(R.id.content_pay_close);

        // 입력 창 초기화 변수
        final String default_text = content_report_memo.getText().toString();
        // 입력 창 클릭
        content_report_memo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(content_report_memo.getText().toString().equals(default_text)) {
                    content_report_memo.setText("");
                    content_report_memo.setTextColor(Color.parseColor("#000000"));
                }
            }
        });

        // 창 닫기
        btn_content_report_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        // 제보하기
        content_report_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String report_memo = content_report_memo.getText().toString();
                try {
                    report_memo = URLEncoder.encode(report_memo,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String test = "https://www.namanapp.co.kr/php/api/parking/php/parking_report.php?parking_code=" + content_code.getText() + "&userid=" +getCurrentUserId()+"&report_text="+report_memo;
                DBConnect task = new DBConnect(test);
                task.start();

                dialog.cancel();
            }
        });
    }

    // 주차장 추가하기
    public void parking_add(){

        // 다이얼로그 리스트 뷰
        dialog = new Dialog(ParkingMain.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.parking_add_dialog);
        dialog.setCanceledOnTouchOutside(false);

        // 주차장 추가 입력창 사이즈 조절
        LinearLayout parking_add_dialog;
        parking_add_dialog = (LinearLayout) dialog.findViewById(R.id.paking_add);
        parking_add_dialog.getLayoutParams().height=(int)Math.round(dm.heightPixels * 0.5);
        parking_add_dialog.getLayoutParams().width=(int)Math.round(dm.widthPixels* 0.85);

        dialog.show();

        // 입력 창 버튼
        final EditText parking_add_memo = (EditText) dialog.findViewById(R.id.parking_add_memo);
        final TextView parking_add_confirm = (TextView) dialog.findViewById(R.id.parking_add_confirm);
        // 닫기 버튼
        ImageView btn_parking_add_close = (ImageView) dialog.findViewById(R.id.content_pay_close);

        // 입력 창 초기화 변수
        final String default_text = parking_add_memo.getText().toString();
        // 입력 창 클릭
        parking_add_memo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(parking_add_memo.getText().toString().equals(default_text)) {
                    parking_add_memo.setText("");
                    parking_add_memo.setTextColor(Color.parseColor("#000000"));
                }
            }
        });

        // 창 닫기
        btn_parking_add_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        // 주차장 추가 요청
        parking_add_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String add_memo = parking_add_memo.getText().toString();
                try {
                    add_memo = URLEncoder.encode(add_memo,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String test = "https://www.namanapp.co.kr/php/api/parking/php/parking_add.php?userid=" +getCurrentUserId()
                        + "&add_text="+add_memo+ "&lat="+here_lat+ "&loc="+here_loc;
                DBConnect task = new DBConnect(test);
                task.start();

                dialog.cancel();
            }
        });

    }

    // 네비게이션 연결
    public void MoveNavi() {
        // 아래 세부창 닫음
        content.setVisibility(View.GONE); // or GONE

        // 다이얼로그 리스트 뷰
        dialog = new Dialog(ParkingMain.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.navi_dialog);

        // 내비게이션 선택창 사이즈 조절
        LinearLayout navi_main_view;
        navi_main_view = (LinearLayout) dialog.findViewById(R.id.navi_main_view);
        navi_main_view.getLayoutParams().height=(int)Math.round(dm.heightPixels * 0.55);
        navi_main_view.getLayoutParams().width=(int)Math.round(dm.widthPixels* 0.85);

        dialog.show();

        // 각 네비 클릭 이벤트 등록);
        LinearLayout navi_dg1 = (LinearLayout) dialog.findViewById(R.id.navi_dg1);
        LinearLayout navi_dg2 = (LinearLayout) dialog.findViewById(R.id.navi_dg2);
        LinearLayout navi_dg3 = (LinearLayout) dialog.findViewById(R.id.navi_dg3);
        LinearLayout navi_dg4 = (LinearLayout) dialog.findViewById(R.id.navi_dg4);

        // 닫기 버튼
        ImageView btn_content_navi_close = (ImageView) dialog.findViewById(R.id.content_navi_close);

        // 티맵만 미리 변수 선언하여 깔렸는지 확인
        final TMapTapi tmaptapi = new TMapTapi(ParkingMain.this);
        /*
        if(tmaptapi.isTmapApplicationInstalled()){
            TextView tmap_text = (TextView) dialog.findViewById(R.id.navi_dg_text2);
            tmap_text.setText("티맵 (설치필요)");
        }
        */

        // 길찾기모드
        navi_dg1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoveMyLocation();
                dialog.cancel();
            }
        });
        // 티맵
        navi_dg2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                tmaptapi.setSKPMapAuthentication ("6e546ec6-7b2f-313e-90c3-4be46df0dbb4");
                tmaptapi.invokeRoute(content_title.getText().toString(), navi_loc, navi_lat);
            }
        });
        // 카카오맵 (해시키 빌드할때마다 등록해야함)
        navi_dg3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                Location destination = Location.newBuilder(content_title.getText().toString(), navi_loc, navi_lat).build();
                KakaoNaviParams.Builder builder = KakaoNaviParams.newBuilder(destination)
                        .setNaviOptions(NaviOptions.newBuilder().setCoordType(CoordType.WGS84).build());
                KakaoNaviService.shareDestination(ParkingMain.this, builder.build());
            }
        });
        // 주소를 클립보드에 복사
        navi_dg4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("도착지복사", content_addr.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ParkingMain.this, "주소복사 : " + content_addr.getText(), Toast.LENGTH_LONG).show();
            }
        });
        // 창 닫기
        btn_content_navi_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
    }

    // 도움말 열기
    public void tutorial() {
        // 아래 세부창 닫음
        content.setVisibility(View.GONE); // or GONE

        // 도움말 창 활성화
        if(park_guide_view.getVisibility()==View.GONE) {
            park_guide_view.setVisibility(View.VISIBLE);
            // 깜박이는 애니메이션
            Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
            park_guide_view.startAnimation(startAnimation);
        }
        else{
            park_guide_view.setVisibility(View.GONE );
        }

    }

    /* 현재 위치 변경 시 호출되는 콜백 인터페이스를 설정한다. */
    private final NMapLocationManager.OnLocationChangeListener onMyLocationChangeListener = new NMapLocationManager.OnLocationChangeListener() {
        @Override
        public boolean onLocationChanged(NMapLocationManager locationManager, NGeoPoint myLocation) {
            if (mMapController != null) {
                mMapController.animateTo(myLocation);
            }
            return true;
        }

        @Override
        public void onLocationUpdateTimeout(NMapLocationManager locationManager) {
        }

        @Override
        public void onLocationUnavailableArea(NMapLocationManager locationManager, NGeoPoint myLocation) {
            stopMyLocation();
        }
    };

    /**
     * Container view class to rotate map view.
     */
    private class MapContainerView extends ViewGroup {
        public MapContainerView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int width = getWidth();
            final int height = getHeight();
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);
                final int childWidth = view.getMeasuredWidth();
                final int childHeight = view.getMeasuredHeight();
                final int childLeft = (width - childWidth) / 2;
                final int childTop = (height - childHeight) / 2;
                view.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            }
            if (changed) {
                mOverlayManager.onSizeChanged(width, height);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            int sizeSpecWidth = widthMeasureSpec;
            int sizeSpecHeight = heightMeasureSpec;

            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);

                if (view instanceof NMapView) {
                    if (mMapView.isAutoRotateEnabled()) {
                        int diag = (((int) (Math.sqrt(w * w + h * h)) + 1) / 2 * 2);
                        sizeSpecWidth = MeasureSpec.makeMeasureSpec(diag, MeasureSpec.EXACTLY);
                        sizeSpecHeight = sizeSpecWidth;
                    }
                }
                view.measure(sizeSpecWidth, sizeSpecHeight);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    // 마커 클릭 리스너
    private final NMapPOIdataOverlay.OnStateChangeListener onPOIdataStateChangeListener = new NMapPOIdataOverlay.OnStateChangeListener() {
        String items[] = new String[1];

        @Override
        public void onCalloutClick(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCalloutClick: title=" + item.getTitle());
            }
            // [[TEMP]] handle a click event of the callout
            // Toast.makeText(ParkingMain.this, "onCalloutClick: " + item.getTitle(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFocusChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (item != null && item.getTitle() != null) {
                // 현재 지도 중심위치를 가져온다
                double center_lat=mMapController.getMapCenter().getLatitude();
                double center_loc=mMapController.getMapCenter().getLongitude();

                final double sinlat = Math.sin(Math.toRadians(center_lat));
                final double coslat = Math.cos(Math.toRadians(center_lat));
                final double sinloc = Math.sin(Math.toRadians(center_loc));
                final double cosloc = Math.cos(Math.toRadians(center_loc));

                String sql = "select parking_code, parking_name, addr, tel, parking_time, latitude, longtitude, default_pay," +
                        "add_pay, day_pay, month_pay, saturday_pay, holiday_pay, one_hour, two_hour, six_hour, (sin_lat_rad * "+sinlat+ "+ cos_lat_rad * "+coslat+" * (sin_loc_rad * "+sinloc+" + cos_loc_rad * "+cosloc+")) as 'distance' " +
                        " from " +tableName+ " where parking_name = '"+item.getTitle()+"';";

                Cursor result = db.rawQuery(sql, null);

                // result(Cursor 객체)가 비어 있으면 false 리턴
                if(result.getCount()>0){
                    result.moveToFirst();

                    content_code.setText(result.getString(0));  // parking_code
                    content_title.setText(result.getString(1)); // parking_name
                    content_addr.setText(result.getString(2));  // parking_addr
                    content_tel.setText(result.getString(3));   // parking_tel
                    content_time.setText(result.getString(4));  // parking_time
                    navi_lat = result.getFloat(5);  // lat
                    navi_loc = result.getFloat(6);  // loc
                    pay_default = result.getString(7); // default_pay
                    pay_add = result.getString(8);     // add_pay
                    pay_day = result.getString(9);     // day_pay
                    pay_month = result.getString(10);     // month_pay
                    free_sat = result.getString(11);     // saturday free yn
                    free_holi = result.getString(12);     // holiday free yn
                    pay_one_hour = result.getString(13);     // 1hour_pay
                    pay_two_hour = result.getString(14);     // 2hour_pay
                    pay_six_hour = result.getString(15);     // 6hour_pay
                    content_distance.setText("지도중심에서" + (int)(6371*Math.acos(result.getDouble(16))*1000) + "m");     // distance

                }
                result.close();

                if(content.getVisibility()==View.GONE) {
                    content.startAnimation(slideUp);
                    content.setVisibility(View.VISIBLE);
                    btn_user_add.setVisibility(View.GONE);
                }
                // 현재 주차 여유공간 정보 업데이트
                // Thread 실행
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            DB_ParkingUpd();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();


            }/*
            else{
                content.setVisibility(View.GONE); // or GONE
            }*/

            if (DEBUG) {
                if (item != null) {
                    Log.i(LOG_TAG, "onFocusChanged: " + item.toString());
                } else {
                    Log.i(LOG_TAG, "onFocusChanged: ");
                }
            }
        }
    };

    // 지도 이동, 확대, 축소 이벤트 리스너
	/* MapView State Change Listener*/
    private final NMapView.OnMapStateChangeListener onMapViewStateChangeListener = new NMapView.OnMapStateChangeListener() {
        int max_user_con=0;

        @Override
        public void onMapInitHandler(NMapView mapView, NMapError errorInfo) {
            if (errorInfo == null) { // success
                // restore map view state such as map center position and zoom level.
                //  restoreInstanceState(); 안써서 주석처리함 맵 초기화후 호출

            } else { // fail
                Log.e(LOG_TAG, "onFailedToInitializeWithError: " + errorInfo.toString());
            }
        }

        @Override
        public void onAnimationStateChange(NMapView mapView, int animType, int animState) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onAnimationStateChange: animType=" + animType + ", animState=" + animState);
            }
        }

        @Override
        public void onMapCenterChange(NMapView mapView, NGeoPoint center) {
            /*
            // 한 유저가 계속 DB접근하면 앱을 종료한다.
            max_user_con=max_user_con+1;
            if(max_user_con>MAX_USER_CALL_DB) {
                finish();
            }
            */
            // DB 연동
            here_lat=center.getLatitude();
            here_loc=center.getLongitude();
            select_marker(here_lat, here_loc);

            if (DEBUG) {
                Log.i(LOG_TAG, "onMapCenterChange: center=" + center.toString());
            }
        }

        @Override
        public void onZoomLevelChange(NMapView mapView, int level) {
           // Toast.makeText(ParkingMain.this, Integer.toString(mMapController.getZoomLevel()), Toast.LENGTH_LONG).show();
            if(level < 8) {
                Toast.makeText(ParkingMain.this, "너무 작은 지도에서는 주차장 정보가 표시되지 않습니다", Toast.LENGTH_LONG).show();
            }
            // DB 연동
           // DB_ParkingInfo(here_lat, here_loc);
        }

        @Override
        public void onMapCenterChangeFine(NMapView mapView) {

        }
    };

    // DB 연결
    private void DB_ParkingInfo() {
        String test = "https://www.namanapp.co.kr/php/api/parking/php/map_select.php";
        DBConnect task = new DBConnect(test);
        task.start();

        try{
            task.join();
        }
        catch(InterruptedException e){
        }
        String result = task.getResult();
        JSON_Mark(result);
    }

    // 현재 주차장 자리 여부 DB 업데이트
    private void DB_ParkingUpd() {
        String test = "https://www.namanapp.co.kr/php/api/parking/php/map_update.php";
        DBConnect task = new DBConnect(test);
        task.start();

        try{
            task.join();
        }
        catch(InterruptedException e){
        }
        String result = task.getResult();

        int cur_parking;
        String parking_code;

        try {
            JSONObject json = new JSONObject(result);
            JSONArray arr = json.getJSONArray("result");

            for(int i = 0; i < arr.length(); i++) {
                JSONObject json2 = arr.getJSONObject(i);
                try {
                    parking_code = json2.getString("parking_code");
                    cur_parking = json2.getInt("cur_parking");

                    // DB 업데이트
                    updateData(parking_code, cur_parking);

                } catch (NumberFormatException nfe) {
                    Log.d(LOG_TAG, "ssong_error" + nfe);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // JSON 데이터를 파싱합니다.
    // URLConnector로부터 받은 String이 JSON 문자열이기 때문입니다.
    private void JSON_Mark(String result){
        try{
            JSONObject json = new JSONObject(result);
            JSONArray arr = json.getJSONArray("result");

            // Markers for POI item
            double longtitude, latitude, sin_lat_rad, sin_loc_rad, cos_lat_rad, cos_loc_rad;
            int last_data_num;
            int cur_parking;
            String parking_code, parking_name, parking_addr, parking_tel, parking_time, default_pay, add_pay, day_pay, month_pay, saturday_pay
            , holiday_pay, one_hour_pay, two_hour_pay, six_hour_pay, content_distance;

            for(int i = 0; i < arr.length(); i++){
                JSONObject json2 = arr.getJSONObject(i);
                try {
                    longtitude=Double.parseDouble(json2.getString("longtitude"));
                    latitude=Double.parseDouble(json2.getString("latitude"));
                    sin_lat_rad=Double.parseDouble(json2.getString("sin_lat_rad"));
                    sin_loc_rad=Double.parseDouble(json2.getString("sin_loc_rad"));
                    cos_lat_rad=Double.parseDouble(json2.getString("cos_lat_rad"));
                    cos_loc_rad=Double.parseDouble(json2.getString("cos_loc_rad"));
                    cur_parking=json2.getInt("cur_parking");
                    parking_code=json2.getString("parking_code");
                    parking_name=json2.getString("parking_name");
                    parking_addr=json2.getString("addr");
                    parking_tel=json2.getString("tel");
                    parking_time=json2.getString("parking_time");
                    default_pay=json2.getString("default_pay");
                    add_pay=json2.getString("add_pay");
                    day_pay=json2.getString("day_pay");
                    month_pay=json2.getString("month_pay");
                    saturday_pay=json2.getString("saturday_pay");
                    holiday_pay=json2.getString("holiday_pay");
                    one_hour_pay=json2.getString("one_hour");
                    two_hour_pay=json2.getString("two_hour");
                    six_hour_pay=json2.getString("six_hour");
                    content_distance=json2.getString("distance");

                    // DB 입력
                    insertData(parking_code, parking_name, parking_addr, parking_tel, latitude, longtitude, sin_lat_rad, sin_loc_rad, cos_lat_rad, cos_loc_rad, cur_parking
                            , content_distance, parking_time, default_pay, add_pay, one_hour_pay, two_hour_pay, six_hour_pay, day_pay, month_pay, saturday_pay, holiday_pay);

                } catch (NumberFormatException nfe) {
                    Log.d(LOG_TAG, "ssong_error" + nfe);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
/**
    // 권한 체크
    boolean isGrantStorage = grantExternalStoragePermission();

    if(isGrantStorage){
        // 일반처리.
    }

    private boolean grantExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            }else{
                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

                return false;
            }
        }else{
            Toast.makeText(this, "External Storage Permission is Grant", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "External Storage Permission is Grant ");
            return true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= 23) {
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                //resume tasks needing this permission
            }
        }
    }
 */ // 접은주석: 권한체크 직적 만드는 법

    // 말풍선 표시 안하기 위한 리스너
    private final NMapOverlayManager.OnCalloutOverlayViewListener onCalloutOverlayViewListener = new NMapOverlayManager.OnCalloutOverlayViewListener() {
        @Override
        public View onCreateCalloutOverlayView(NMapOverlay itemOverlay, NMapOverlayItem overlayItem, Rect itemBounds) {
            // null을 반환하면 말풍선 오버레이를 표시하지 않음
            return null;
        }

    };

    // 지도 터치 이벤트 리스너
    private final NMapView.OnMapViewTouchEventListener onMapViewTouchEventListener = new NMapView.OnMapViewTouchEventListener() {
        @Override
        public void onLongPress(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onLongPressCanceled(NMapView mapView) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onSingleTapUp(NMapView mapView, MotionEvent ev) {
            if(content.getVisibility()==View.VISIBLE) {
                content.startAnimation(slideDown);
                content.setVisibility(View.GONE); // or GONE
            }
            // TODO Auto-generated method stub
        }
        @Override
        public void onTouchDown(NMapView mapView, MotionEvent ev) {
        }
        @Override
        public void onScroll(NMapView mapView, MotionEvent e1, MotionEvent e2) {
            if(content.getVisibility()==View.VISIBLE) {
                content.startAnimation(slideDown);
                content.setVisibility(View.GONE); // or GONE

            }
        }
        @Override
        public void onTouchUp(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub
        }
    };

    // 유저 디바이스 아이디 가져오기
    public String getCurrentUserId(){
        String deviceId="";
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return deviceId;
    }


    // Database 생성 및 열기
    public void createDatabase(String dbName, int dbMode){
        db = openOrCreateDatabase(dbName,dbMode,null);
    }

    // Table 생성
    public void createTable(){
        String sql = "create table if not exists " + tableName + "(parking_code Integer primary key, "+"parking_name text null, "+"addr text not null" +
                ", "+"tel text null, "+"latitude double null, "+"longtitude double null, "+"sin_lat_rad double null, "+"sin_loc_rad double null" +
                ", "+"cos_lat_rad double null, "+"cos_loc_rad double null, "+"cur_parking int null" +
                ", "+"distance text null, "+"parking_time text null" +
                ", "+"default_pay text null, "+"add_pay text null, "+"one_hour text null, "+"two_hour text null" +
                ", "+"six_hour text null, "+"day_pay text null, "+"month_pay text null, "+"saturday_pay text null" +
                ", "+"holiday_pay text null)";
        db.execSQL(sql);
    }

    // Table 삭제
    public void removeTable(){
        String sql = "drop table " + tableName;
        db.execSQL(sql);
    }


    // Data 추가
    public void insertData(String parking_code, String parking_name, String addr, String tel, double latitude, double longtitude, double sin_lat_rad
            , double sin_loc_rad, double cos_lat_rad, double cos_loc_rad, int cur_parking
            , String distance, String parking_time, String default_pay, String add_pay
            , String one_hour, String two_hour, String six_hour, String day_pay, String month_pay, String saturday_pay, String holiday_pay){
        String sql = "insert or replace into " + tableName +
                " values('"+parking_code+"', '"+parking_name+"', '"+addr+"', '"+tel+"', '"+latitude+"', '"+longtitude+"', '"+sin_lat_rad+"', '"+sin_loc_rad+"'" +
                ", '"+cos_lat_rad+"', '"+cos_loc_rad+"', '"+cur_parking+"', '"+distance+"'" +
                ", '"+parking_time+"', '"+default_pay+"', '"+add_pay+"', '"+one_hour+"'" +
                ", '"+two_hour+"', '"+six_hour+"', '"+day_pay+"', '"+month_pay+"', '"+saturday_pay+"', '"+holiday_pay+"');";
        db.execSQL(sql);
    }

    // Data 업데이트
    public void updateData(String parking_code, int cur_parking){
        String sql = "update " + tableName + " set cur_parking = '" + cur_parking +"' where parking_code = "+parking_code+";";
        db.execSQL(sql);
    }

    // Data 삭제
    public void removeData(){
        String sql = "delete from " + tableName + ";";
        db.execSQL(sql);
    }

    // 모든 Data 삭제
    public void removeAllData(){
        String sql = "select name from sqlite_master WHERE name='" + tableName + "' and type='table';";
        Cursor result = db.rawQuery(sql, null);

        if(result.getCount()>0){
            sql = "delete from " + tableName + " ";
            db.execSQL(sql);
        }
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
    public void select_marker(double here_lat, double here_loc){
        // DB 연결이 끊겼을 떄 다시 연결(홈에서 오래있다 들어오면 끊김)
        if(db == null){
            createDatabase(dbName,dbMode);
        }

        final double sinlat = Math.sin(Math.toRadians(here_lat));
        final double coslat = Math.cos(Math.toRadians(here_lat));
        final double sinloc = Math.sin(Math.toRadians(here_loc));
        final double cosloc = Math.cos(Math.toRadians(here_loc));

        mOverlayManager.clearOverlays();

        String sql = "select parking_name, latitude, longtitude, cur_parking " +
                "from "+ tableName + " " +
                "order by (sin_lat_rad * "+sinlat+ "+ cos_lat_rad * "+coslat+" * (sin_loc_rad * "+sinloc+" + cos_loc_rad * "+cosloc+")) desc " +
                "limit 40;";
        Cursor results = db.rawQuery(sql, null);


        // set POI data
        NMapPOIdata poiData = new NMapPOIdata(results.getCount(), mMapViewerResourceProvider); // 숫자에다가 마지막 행번호
        results.moveToFirst();

        while(!results.isAfterLast()){
            String id = results.getString(0);    // parking_name
            double latitude, longtitude;
            latitude = results.getDouble(1);     // lat
            longtitude = results.getDouble(2);   // lot

            if(results.getInt(3) == 1)  // 자리있는 주차장
                poiData.addPOIitem(longtitude, latitude, id, ContextCompat.getDrawable(getBaseContext(), R.drawable.pin_1), 1);
            else if(results.getInt(3) == 2) // 자리없는 주차장
                poiData.addPOIitem(longtitude, latitude, id, ContextCompat.getDrawable(getBaseContext(), R.drawable.pin_2), 1);
            else // 나머지 주차장
                poiData.addPOIitem(longtitude, latitude, id, ContextCompat.getDrawable(getBaseContext(), R.drawable.pin_0), 1);
            // Toast.makeText(ParkingMain.this, results.getString(1), Toast.LENGTH_LONG).show();
            results.moveToNext();
        }

        results.close();

        // 검색창으로 이동시 마커 표시
        if(search_flag==true){
            // 검색으로 위치변경시 가운데 마커 표시
            poiData.addPOIitem(search_lat, search_loc, null, NMapPOIflagType.PIN, 1);
        }
        // 마커 종료
        poiData.endPOIdata();

        // create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);
        // set event listener to the overlay
        poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);
        // select an item
        // poiDataOverlay.selectPOIitem(0, true);

    }
}