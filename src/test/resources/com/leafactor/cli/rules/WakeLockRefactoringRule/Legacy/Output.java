package test.resources.com.leafactor.cli.rules.WakeLockRefactoringRule.Legacy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WakeLockSample {
    public class SimpleWakeLockActivity extends Activity {
        private PowerManager.WakeLock wl;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLockSample");
            wl.acquire();
        }

        @Override
        protected void onPause() {
            super.onPause();
            if (!wl.isHeld()) {
                wl.release();
            }
        }

        @Override
        public void onDestroy(){
            super.onDestroy();
        }
    }

    public class SimpleWakeLockWithoutOnPauseActivity extends Activity {
        private PowerManager.WakeLock wl;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLockSample");
            wl.acquire();
        }

        @Override
        public void onDestroy(){
            super.onDestroy();
        }

        @Override() protected void onPause(){
            super.onPause();
            if (!wl.isHeld()) {
                wl.release();
            }
        }
    }

    public class SimpleWakeLockWithoutReleaseActivity extends Activity {
        private PowerManager.WakeLock wl;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLockSample");
            wl.acquire();
        }

        @Override() protected void onPause(){
            super.onPause();
            if (!wl.isHeld()) {
                wl.release();
            }
        }
    }

    public class SimpleWakeLockWithoutReleaseAndWithoutOnPauseActivity extends Activity {
        private PowerManager.WakeLock wl;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLockSample");
            wl.acquire();
        }

        @Override() protected void onPause(){
            super.onPause();
            if (!wl.isHeld()) {
                wl.release();
            }
        }
    }
}