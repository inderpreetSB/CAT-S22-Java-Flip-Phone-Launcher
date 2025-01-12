package com.example.minimal_dpad_launcher;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//Imports for our code (other than default imports that come with android studio)
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView; // This will display the list of apps.
    private AppListAdapter appListAdapter; // Adapter to manage app items in RecyclerView.
    private LinearLayoutManager layoutManager; // Layout manager for RecyclerView.
    private List<AppModel> appList = new ArrayList<>(); // List to hold app data.
    private SharedPreferences prefs; // For saving app settings (like hidden/renamed apps).

    // This is the onCreate() method, called when the app first opens.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Setting the layout for our activity.

        // Find RecyclerView in layout and set it up with a LinearLayoutManager.
        recyclerView = findViewById(R.id.appList);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        prefs = getSharedPreferences("launcherPrefs", Context.MODE_PRIVATE); // Initialize preferences for saving app settings.

        loadAppList(); // Load the list of apps installed on the device.
        loadAppSettings(); // Apply saved settings like hidden or renamed apps.

        // Set up the adapter to display apps in RecyclerView.
        appListAdapter = new AppListAdapter(appList);
        recyclerView.setAdapter(appListAdapter);
    }

    // This method is called every time the app becomes visible again.
    @Override
    protected void onResume() {
        super.onResume();
        refreshAppList(); // Reload the app list to show any recent app changes.
    }

    // Save app settings (hidden/renamed apps) when the app is paused.
    @Override
    protected void onPause() {
        super.onPause();
        saveAppSettings();
    }

    // This method loads all installed apps and their info (name, icon, package).
    private void loadAppList() {
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER); // Find launchable apps.
        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);

        for (ResolveInfo app : apps) { // For each app, get its label, icon, and package name.
            String label = app.loadLabel(packageManager).toString();
            Drawable icon = app.loadIcon(packageManager);
            String packageName = app.activityInfo.packageName;
            appList.add(new AppModel(label, icon, packageName)); // Add app to the list.
        }
    }

    // Load saved settings like hidden/renamed apps from SharedPreferences.
    private void loadAppSettings() {
        for (AppModel app : appList) {
            boolean isHidden = prefs.getBoolean(app.getPackageName() + "_hidden", false); // Check if the app is hidden.
            String customName = prefs.getString(app.getPackageName() + "_customName", app.getName()); // Get the custom name, if any.
            app.setHidden(isHidden);
            app.setCustomName(customName);
        }
    }

    // Save current settings for hidden/renamed apps.
    private void saveAppSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        for (AppModel app : appList) {
            editor.putBoolean(app.getPackageName() + "_hidden", app.isHidden());
            editor.putString(app.getPackageName() + "_customName", app.getCustomName());
        }
        editor.apply(); // Apply the changes to save.
    }

    // Refresh the app list to show any new or removed apps.
    private void refreshAppList() {
        List<AppModel> updatedList = new ArrayList<>(appList); // Keep a copy of current list.
        loadAppList(); // Reload app list.
        loadAppSettings(); // Reapply saved settings.
        appListAdapter.notifyDataSetChanged(); // Update the RecyclerView.
    }

    // Launch an app when the user selects it with the center D-pad button.
    private void launchApp(int position) {
        if (position < 0 || position >= appList.size()) return;
        AppModel app = appList.get(position);
        if (app.isHidden()) return; // Ignore if the app is hidden

        String packageName = app.getPackageName();
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            startActivity(intent);
        }
    }


    // Handle D-pad navigation and selection with key events.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int currentPosition = layoutManager.findFirstVisibleItemPosition();

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Scroll down by one item, if possible
                if (currentPosition < appList.size() - 1) {
                    recyclerView.smoothScrollToPosition(currentPosition + 1);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                // Scroll up by one item, if possible
                if (currentPosition > 0) {
                    recyclerView.smoothScrollToPosition(currentPosition - 1);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Placeholder for additional functionality (e.g., switching screens/tabs)
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Placeholder for additional functionality (e.g., switching screens/tabs)
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // Launch the app at the current position
                launchApp(currentPosition);
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }


    // Adapter class to connect app data with RecyclerView UI.
    private class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
        private final List<AppModel> apps;

        public AppListAdapter(List<AppModel> apps) {
            this.apps = apps;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
            return new ViewHolder(view); // Create a new view holder.
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppModel app = apps.get(position);
            holder.textView.setText(app.getName());
            holder.textView.setOnClickListener(v -> {
                // Launch the app when the text is clicked
                Intent launchIntent = v.getContext().getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                if (launchIntent != null) {
                    v.getContext().startActivity(launchIntent);
                } else {
                    Toast.makeText(v.getContext(), "Unable to launch app", Toast.LENGTH_SHORT).show();
                }
            });

            holder.textView.setVisibility(app.isHidden() ? View.GONE : View.VISIBLE); // Handle hidden apps
        }

        @Override
        public int getItemCount() {
            return (int) apps.stream().filter(app -> !app.isHidden()).count(); // Count only visible apps.
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
            TextView textView;

            ViewHolder(View view) {
                super(view);
                textView = view.findViewById(R.id.appName);
                view.setOnLongClickListener(this); // Enable long-click for app options.
            }

            // Handle long-click to show options like Hide or Rename for apps.
            @Override
            public boolean onLongClick(View v) {
                int position = getAdapterPosition();
                AppModel app = apps.get(position);

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Options")
                        .setItems(new CharSequence[]{"Hide", "Rename"}, (dialog, which) -> {
                            if (which == 0) { // Hide option
                                app.setHidden(true);
                                notifyDataSetChanged();
                            } else if (which == 1) { // Rename option
                                renameApp(app);
                            }
                        }).show();
                return true;
            }

            // Show a dialog to rename the selected app.
            private void renameApp(AppModel app) {
                EditText input = new EditText(itemView.getContext());
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Rename App")
                        .setView(input)
                        .setPositiveButton("OK", (dialog, which) -> {
                            String newName = input.getText().toString().trim();
                            if (!newName.isEmpty()) {
                                app.setCustomName(newName); // Set the custom name.
                                notifyDataSetChanged(); // Update UI.
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    // Model class to represent an app's information.
    public static class AppModel {
        private final String name;
        private

        final Drawable icon;
        private final String packageName;
        private boolean hidden;
        private String customName;

        public AppModel(String name, Drawable icon, String packageName) {
            this.name = name;
            this.icon = icon;
            this.packageName = packageName;
            this.customName = name;
            this.hidden = false;
        }

        public String getName() {
            return customName;
        }

        public Drawable getIcon() {
            return icon;
        }

        public String getPackageName() {
            return packageName;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public String getCustomName() {
            return customName;
        }

        public void setCustomName(String customName) {
            this.customName = customName;
        }
    }
}
