package com.example.qcameraq;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPre {
    Context context;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor = sharedPreferences.edit();


    public SharedPre(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("APP_SHAREDPRE", Context.MODE_PRIVATE);
    }

    /*
    face == true => Back
    face == false => Front
     */
    public void setFace(boolean face){
        editor.putBoolean("face", face);
        editor.apply();
    }

    /*
    flash == 0 => Off
    flash == 1 => On
    flash == 2 => Auto
     */
    public void setFlash(int flash){
        editor.putInt("flash", flash);
        editor.apply();
    }

    /*
    grid == 0 => Off
    grid == 1 => 3*3
    grid == 2 => 4*4
    grid == 3 => Grid Phi
     */
    public void setGrid(int Grid){
        editor.putInt("grid", Grid);
        editor.apply();
    }

    public boolean getRotated(){
        return sharedPreferences.getBoolean("rotated", true);
    }

    public int getFlash(){
        return sharedPreferences.getInt("flash", 2);
    }

    public int getGrid() { return sharedPreferences.getInt("grid", 0); }
}
