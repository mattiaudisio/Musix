package com.example.musix;

import static com.example.musix.AlbumDetailsAdapter.albumFiles;
import static com.example.musix.MainActivity.repeatBoolean;
import static com.example.musix.MainActivity.shuffleBoolean;
import static com.example.musix.MusicAdapter.mFiles;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {

    TextView song_name, artist_name, duration_played, duration_total;
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;
    int position = -1;
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;
    // static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Thread playThread, prevThread, nextThread;
    MusicService musicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFulscreen();
        setContentView(R.layout.activity_player);
        getSupportActionBar().hide();
        initViews();
        getIntenMethod();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser){
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null){
                    int  mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this,1000);
            }
        });
        shuffleBtn.setOnClickListener(v -> {
            if (shuffleBoolean){
                shuffleBoolean = false;
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
            }else{
                shuffleBoolean = true;
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            }
        });
        repeatBtn.setOnClickListener(v -> {
            if (repeatBoolean){
                repeatBoolean = false;
                repeatBtn.setImageResource(R.drawable.ic_repeat_off);
            }else{
                repeatBoolean = true;
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
            }
        });
        backBtn.setOnClickListener(v -> onBackPressed());

    }

    private void setFulscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this,MusicService.class);
        bindService(intent,this,BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void prevThreadBtn() {
        prevThread = new Thread(){
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(v -> btn_prevClicked());
            }
        };
        prevThread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void btn_prevClicked() {
        if (musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }else if (!shuffleBoolean && !repeatBoolean){
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position - 1));
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int  mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }else if (!shuffleBoolean && !repeatBoolean){
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position - 1));
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int  mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_play);
            playPauseBtn.setBackgroundResource(R.drawable.ic_play);
        }
    }

    private void nextThreadBtn() {
        nextThread = new Thread(){
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(v -> btn_nextClicked());
            }
        };
        nextThread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void btn_nextClicked() {
        if (musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }else if (!shuffleBoolean && !repeatBoolean){
                position = ((position + 1) % listSongs.size());
            }
            // else position will be position...
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int  mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }else if (!shuffleBoolean && !repeatBoolean){
                position = ((position + 1) % listSongs.size());
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int  mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_play);
            playPauseBtn.setBackgroundResource(R.drawable.ic_play);
        }
    }

    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i + 1);
    }

    private void playThreadBtn() {
        playThread = new Thread(){
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                super.run();
                playPauseBtn.setOnClickListener(v -> btn_play_pauseClicked());
            }
        };
        playThread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void btn_play_pauseClicked() {
        if(musicService.isPlaying()){
            playPauseBtn.setImageResource(R.drawable.ic_play);
            musicService.showNotification(R.drawable.ic_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int  mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
        }else{
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            musicService.showNotification(R.drawable.ic_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null){
                        int  mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
        }
    }

    private String formattedTime(int mCurrentPosition) {
        String totalout = "", totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalout = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1){
            return totalNew;
        }else{
            return totalout;
        }
    }

    private void getIntenMethod() {
        position = getIntent().getIntExtra("position",-1);
        String sender = getIntent().getStringExtra("sender");
        if (sender != null && sender.equals("albumDetails")){
            listSongs = albumFiles;
        }else{
            listSongs = mFiles;
        }
        if (listSongs != null){
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            uri = Uri.parse(listSongs.get(position).getPath());
        }
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition",position);
        startService(intent);

    }

    private void initViews() {
        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
    }

    private void metaData(Uri uri){
        MediaMetadataRetriever retriever =  new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
        duration_total.setText(formattedTime(durationTotal));
        byte[] art = retriever.getEmbeddedPicture();
        Bitmap bitmap;
        if (art != null){
            bitmap = BitmapFactory.decodeByteArray(art,0,art.length);
            ImageAnimation(this,cover_art,bitmap);
            Palette.from(bitmap).generate(palette -> {
                Palette.Swatch swatch = palette.getDominantSwatch();
                if (swatch != null) {
                    ImageView gredient = findViewById(R.id.imageViewGredient);
                    RelativeLayout mContainer = findViewById(R.id.mContainer);
                    gredient.setBackgroundResource(R.drawable.gredient_bg);
                    mContainer.setBackgroundResource(R.drawable.main_bg);
                    GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {swatch.getRgb(),0x00000000});
                    gredient.setBackground(gradientDrawable);
                    GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {swatch.getRgb(),swatch.getRgb()});
                    mContainer.setBackground(gradientDrawableBg);
                    song_name.setTextColor(swatch.getTitleTextColor());
                    artist_name.setTextColor(swatch.getBodyTextColor());
                }else{
                    ImageView gredient = findViewById(R.id.imageViewGredient);
                    RelativeLayout mContainer = findViewById(R.id.mContainer);
                    gredient.setBackgroundResource(R.drawable.gredient_bg);
                    mContainer.setBackgroundResource(R.drawable.main_bg);
                    GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {0xff000000,0x00000000});
                    gredient.setBackground(gradientDrawable);
                    GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {0xff000000,0xff000000});
                    mContainer.setBackground(gradientDrawableBg);
                    song_name.setTextColor(Color.WHITE);
                    artist_name.setTextColor(Color.DKGRAY);
                }
            });
        }else{
            Glide.with(this).asBitmap().load(R.drawable.default_image).into(cover_art);
            ImageView gredient = findViewById(R.id.imageViewGredient);
            RelativeLayout mContainer = findViewById(R.id.mContainer);
            gredient.setBackgroundResource(R.drawable.gredient_bg);
            mContainer.setBackgroundResource(R.drawable.main_bg);
            song_name.setTextColor(Color.WHITE);
            artist_name.setTextColor(Color.DKGRAY);
        }

    }

    public void ImageAnimation(Context context, ImageView imageView,Bitmap bitmap){
        Animation animOut = AnimationUtils.loadAnimation(context,android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context,android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);
        seekBar.setMax(musicService.getDuration() / 1000);
        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        musicService.onCompleted();
        musicService.showNotification(R.drawable.ic_pause);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }
}