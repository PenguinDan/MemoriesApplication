package edu.csulb.memoriesapplication;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/**
 * Created by Daniel on 11/3/2017.
 * Checks the user's permission based on the permission defined in the enumerator
 */

public class UserPermission {

    public enum Permission{
        READ_PERMISSION,
        LOCATION_PERMISSION
    }

    public static boolean checkUserPermission(Context context, Permission permission) {
        switch(permission) {
            case READ_PERMISSION:{
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED){
                    return true;
                }
            }
            break;
            case LOCATION_PERMISSION:{
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
            break;
        }
        return false;
    }
}
