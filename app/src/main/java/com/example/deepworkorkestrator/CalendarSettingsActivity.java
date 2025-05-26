package com.example.deepworkorkestrator;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarSettingsActivity extends AppCompatActivity {
    private static final String TAG = "CalendarSettingsActivity";
    private static final int CALENDAR_PERMISSION_REQUEST = 1;
    private EditText eventTitleEditText;
    private EditText eventDescriptionEditText;
    private CheckBox autoStartCheckBox;
    private Button saveSettingsButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_settings);

        sharedPreferences = getSharedPreferences("DeepWorkSettings", Context.MODE_PRIVATE);

        eventTitleEditText = findViewById(R.id.eventTitleEditText);
        eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);
        autoStartCheckBox = findViewById(R.id.autoStartCheckBox);
        saveSettingsButton = findViewById(R.id.saveSettingsButton);

        // Load saved settings with default values
        String defaultEventTitle = "Głęboka praca";
        eventTitleEditText.setText(sharedPreferences.getString("calendarEventTitle", defaultEventTitle));
        eventDescriptionEditText.setText(sharedPreferences.getString("calendarEventDescription", ""));
        autoStartCheckBox.setChecked(sharedPreferences.getBoolean("autoStartFromCalendar", false));

        // If no title is set, set the default
        if (!sharedPreferences.contains("calendarEventTitle")) {
            sharedPreferences.edit().putString("calendarEventTitle", defaultEventTitle).apply();
        }

        saveSettingsButton.setOnClickListener(v -> saveSettings());

        // Check calendar permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    },
                    CALENDAR_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALENDAR_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
            } else {
                Toast.makeText(this, "Wymagane uprawnienia do kalendarza", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("calendarEventTitle", eventTitleEditText.getText().toString());
        editor.putString("calendarEventDescription", eventDescriptionEditText.getText().toString());
        editor.putBoolean("autoStartFromCalendar", autoStartCheckBox.isChecked());
        editor.apply();

        Toast.makeText(this, "Ustawienia zapisane", Toast.LENGTH_SHORT).show();
    }

    public static List<CalendarEvent> getUpcomingDeepWorkEvents(Context context) {
        List<CalendarEvent> events = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        SharedPreferences prefs = context.getSharedPreferences("DeepWorkSettings", Context.MODE_PRIVATE);
        String eventTitle = prefs.getString("calendarEventTitle", "Głęboka praca");

        String[] projection = new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
        };

        String selection = CalendarContract.Events.TITLE + " LIKE ? AND " +
                CalendarContract.Events.DTSTART + " > ?";
        String[] selectionArgs = new String[]{
                "%" + eventTitle + "%",
                String.valueOf(System.currentTimeMillis())
        };

        Cursor cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                CalendarContract.Events.DTSTART + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String description = cursor.getString(2);
                long startTime = cursor.getLong(3);
                long endTime = cursor.getLong(4);

                events.add(new CalendarEvent(id, title, description, startTime, endTime));
            }
            cursor.close();
        }

        return events;
    }

    public static List<CalendarEvent> getCurrentDeepWorkEvents(Context context) {
        List<CalendarEvent> events = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        SharedPreferences prefs = context.getSharedPreferences("DeepWorkSettings", Context.MODE_PRIVATE);
        String eventTitle = prefs.getString("calendarEventTitle", "Głęboka praca");
        long currentTime = System.currentTimeMillis();

        String[] projection = new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
        };

        String selection = CalendarContract.Events.TITLE + " LIKE ? AND " +
                CalendarContract.Events.DTSTART + " <= ? AND " +
                CalendarContract.Events.DTEND + " >= ?";
        String[] selectionArgs = new String[]{
                "%" + eventTitle + "%",
                String.valueOf(currentTime),
                String.valueOf(currentTime)
        };

        Cursor cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                CalendarContract.Events.DTSTART + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String description = cursor.getString(2);
                long startTime = cursor.getLong(3);
                long endTime = cursor.getLong(4);

                events.add(new CalendarEvent(id, title, description, startTime, endTime));
            }
            cursor.close();
        }

        return events;
    }

    public static class CalendarEvent {
        public final long id;
        public final String title;
        public final String description;
        public final long startTime;
        public final long endTime;

        public CalendarEvent(long id, String title, String description, long startTime, long endTime) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static class UpcomingEventsAdapter extends RecyclerView.Adapter<UpcomingEventsAdapter.EventViewHolder> {
        private List<CalendarEvent> events = new ArrayList<>();

        public void setEvents(List<CalendarEvent> events) {
            this.events = events;
            notifyDataSetChanged();
        }

        @Override
        public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_event, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(EventViewHolder holder, int position) {
            CalendarEvent event = events.get(position);
            holder.bind(event);
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            private TextView titleText;
            private TextView timeText;
            private Button openCalendarButton;

            public EventViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.eventTitle);
                timeText = itemView.findViewById(R.id.eventTime);
                openCalendarButton = itemView.findViewById(R.id.openCalendarButton);
            }

            public void bind(CalendarEvent event) {
                titleText.setText(event.title);
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                String timeRange = sdf.format(new Date(event.startTime)) + " - " + 
                                 sdf.format(new Date(event.endTime));
                timeText.setText(timeRange);

                openCalendarButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        itemView.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening calendar", e);
                        Toast.makeText(itemView.getContext(), 
                            "Nie udało się otworzyć kalendarza", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
} 