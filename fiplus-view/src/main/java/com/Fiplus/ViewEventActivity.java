package com.Fiplus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wordnik.client.api.ActsApi;
import com.wordnik.client.api.UsersApi;
import com.wordnik.client.model.Activity;
import com.wordnik.client.model.Attendee;
import com.wordnik.client.model.CreateSuggestionResponse;
import com.wordnik.client.model.Joiner;
import com.wordnik.client.model.Location;
import com.wordnik.client.model.Time;
import com.wordnik.client.model.UserProfile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import adapters.LocationArrayAdapterNoFilter;
import adapters.PendingLocListAdapter;
import adapters.PendingTimeLocListAdapter;
import adapters.SuggestionListAdapter;
import model.PendingLocItem;
import model.PendingTimeItem;
import model.SuggestionListItem;
import utils.DateTimePicker;
import utils.GeoAutoCompleteInterface;
import utils.GeocodingLocation;
import utils.IAppConstants;
import utils.ListViewUtil;
import utils.PrefUtil;

public class ViewEventActivity extends FragmentActivity  implements TextWatcher, GeoAutoCompleteInterface {
    public static final String EXTRA_EVENT_ID = "eventID";

    protected TextView mEventDesc;
    protected AutoCompleteTextView mEventLocation;
    protected Button mJoinEventBtn;
    protected Button mCancelBtn;
    protected ListView mLocationList;
    protected ListView mTimeList;
    protected ListView mSuggestedLocList;
    protected ListView mSuggestedTimeList;
    protected LinearLayout mAttendeesList;
    protected Button mSuggestTimeBtn;
    protected Button mAddLocationBtn;

    ArrayList<PendingLocItem> locPendingSuggestion = new ArrayList<PendingLocItem>();
    private PendingLocListAdapter mPendingLocSuggestionAdapter;

    ArrayList<PendingTimeItem> timePendingSuggestion = new ArrayList<PendingTimeItem>();
    private PendingTimeLocListAdapter mPendingTimeSuggestionAdapter;

    ArrayList<SuggestionListItem> timeSuggestionList = new ArrayList<SuggestionListItem>();
    private SuggestionListAdapter mTimesListAdapter;
    protected List<UserProfile> mAttendees = new ArrayList<UserProfile>();

    ArrayList<SuggestionListItem> locSuggestionList = new ArrayList<SuggestionListItem>();
    protected ArrayAdapter<String> autoCompleteLocationAdapter;
    private SuggestionListAdapter mLocationListAdapter;

    boolean mIsAJoiner = false;
    boolean mIsACreator = false;
    String eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        Bundle b = getIntent().getExtras();
        final String mEventID = b.getString(EXTRA_EVENT_ID);
        eventID = mEventID;

        mEventDesc = (TextView) findViewById(R.id.view_event_description);
        mLocationList = (ListView) findViewById(R.id.view_event_loc_checkboxes);
        mLocationList.setOnTouchListener(new TouchListener());

        mSuggestedLocList = (ListView) findViewById(R.id.view_event_pending_loc);
        mPendingLocSuggestionAdapter = new PendingLocListAdapter(this, locPendingSuggestion);
        mSuggestedLocList.setAdapter(mPendingLocSuggestionAdapter);
        mSuggestedLocList.setOnTouchListener(new TouchListener());
        mSuggestedLocList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                locPendingSuggestion.remove(position);
                mPendingLocSuggestionAdapter.notifyDataSetChanged();
                ListViewUtil.setListViewHeightBasedOnChildren(mSuggestedLocList);

                if(locPendingSuggestion.size() == 0) {
                    mSuggestedLocList.setVisibility(View.GONE);
                }
            }
        });

        mSuggestedTimeList = (ListView) findViewById(R.id.view_event_pending_time);
        mPendingTimeSuggestionAdapter = new PendingTimeLocListAdapter(this, timePendingSuggestion);
        mSuggestedTimeList.setAdapter(mPendingTimeSuggestionAdapter);
        mSuggestedTimeList.setOnTouchListener(new TouchListener());
        mSuggestedTimeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                timePendingSuggestion.remove(position);
                mPendingTimeSuggestionAdapter.notifyDataSetChanged();
                ListViewUtil.setListViewHeightBasedOnChildren(mSuggestedTimeList);

                if(timePendingSuggestion.size() == 0) {
                    mSuggestedTimeList.setVisibility(View.GONE);
                }
            }
        });


        mTimeList = (ListView) findViewById(R.id.view_event_time_checkboxes);
        mTimeList.setOnTouchListener(new TouchListener());

        mAttendeesList = (LinearLayout)findViewById(R.id.view_event_attendees_list);

        GetEventTask getEventTask = new GetEventTask(mEventID);
        getEventTask.execute();

        mJoinEventBtn = (Button) findViewById(R.id.view_event_join_btn);
        mJoinEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JoinEventTask joinEventTask = new JoinEventTask(mEventID);
                joinEventTask.execute();
            }
        });

        //TODO: (Jobelle) Join Event - Change button label based on user (i.e. isCreator or isJoiner)
        mCancelBtn = (Button) findViewById(R.id.view_event_not_interested_btn);
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsACreator)
                {
                    CancelEventTask cancelEventTask = new CancelEventTask(mEventID);
                    cancelEventTask.execute();
                }
                if(mIsAJoiner)
                {
                    UnJoinEventTask unJoinEventTask = new UnJoinEventTask(mEventID);
                    unJoinEventTask.execute();
                }
                finish();
            }
        });

        mSuggestTimeBtn = (Button) findViewById(R.id.view_event_suggest_time);
        mSuggestTimeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                DateTimePicker getDateTime = new DateTimePicker(ViewEventActivity.this);
                getDateTime.showDateTimePickerDialog();
            }
        });

        mEventLocation = (AutoCompleteTextView) findViewById(R.id.view_event_location);
        autoCompleteLocationAdapter = new LocationArrayAdapterNoFilter(this, android.R.layout.simple_dropdown_item_1line);
        autoCompleteLocationAdapter.setNotifyOnChange(false);
        mEventLocation.addTextChangedListener(this);
        mEventLocation.setThreshold(MAX_CHARS);
        mEventLocation.setAdapter(autoCompleteLocationAdapter);


        mAddLocationBtn = (Button) findViewById(R.id.view_event_add_location);
        mAddLocationBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String address = mEventLocation.getText().toString();

                if(!address.isEmpty())
                {
                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) && Geocoder.isPresent())
                    {
                        GeocodingLocation location = new GeocodingLocation(getBaseContext(), ViewEventActivity.this, FINAL_LOC);
                        location.execute(address);
                    }

                }
            }
        });

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_in_from_left, R.anim.activity_out_to_right);
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_in_from_left, R.anim.activity_out_to_right);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final String address = s.toString();

        if(!address.isEmpty() && address.length() >= MAX_CHARS)
        {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) && Geocoder.isPresent())
            {
                GeocodingLocation location = new GeocodingLocation(getBaseContext(), ViewEventActivity.this, AUTOCOMPLETE);
                location.execute(address);
            }
        }
        else
        {
            autoCompleteLocationAdapter.clear();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void populateLocation(Location location, String address) {

        if(address!=null)
        {
            mEventLocation.setText("");
            //if the user is the creator/joiner of an event, automatically add
            //the suggested location
            if(mIsACreator || mIsAJoiner)
            {
                AddSuggestionTask newSuggest = new AddSuggestionTask(location, address);
                newSuggest.execute();
            }
            else //put on pending list first
            {
                locPendingSuggestion.add(new PendingLocItem(address, location));
                mPendingLocSuggestionAdapter.notifyDataSetChanged();
                ListViewUtil.setListViewHeightBasedOnChildren(mSuggestedLocList);

                if(locPendingSuggestion.size() > 0) {
                    mSuggestedLocList.setVisibility(View.VISIBLE);
                }
            }
        }
        else
        {
            mEventLocation.setError(getString(R.string.error_address_not_found));
        }
    }

    @Override
    public ArrayAdapter<String> getAutoComplete() {
            return autoCompleteLocationAdapter;
    }

    //gets called from DateTimePicker
    public void addDateTime(Time time, String sTime)
    {
        //if the user is the creator/joiner of an event, automatically add
        //the suggested time
        if(mIsAJoiner || mIsACreator)
        {
            AddSuggestionTask newSuggest = new AddSuggestionTask(time);
            newSuggest.execute();
        }
        else //add to pending list first
        {
            timePendingSuggestion.add(new PendingTimeItem(convertToTimeToString(time), time));
            mPendingTimeSuggestionAdapter.notifyDataSetChanged();
            ListViewUtil.setListViewHeightBasedOnChildren(mSuggestedTimeList);

            if(timePendingSuggestion.size() > 0) {
                mSuggestedTimeList.setVisibility(View.VISIBLE);
            }
        }
    }

    private String convertToTimeToString(Time time)
    {
        long startDate = time.getStart().longValue();
        long endDate = time.getEnd().longValue();
        int numOfVotes;
        String s;

        //add votes
        try {
            numOfVotes = time.getSuggestion_votes().intValue();
        } catch (NullPointerException e)
        {
            numOfVotes = -1;
        }

        s = " (" + numOfVotes + " vote(s))";

        Date d1 = new Date(startDate);
        Date d2 = new Date(endDate);
        String format = "EEE MMM d, h:m a";

        String format2 = format;
        String middle = " to ";
        boolean curYear = d1.getYear() == new Date().getYear();
        boolean sameMonth = d1.getMonth() == d2.getMonth();
        boolean sameDay = d1.getDate() == d2.getDate();

        if(sameDay && sameMonth)
        {
            format2 = "h:m a";
            middle = " to ";
        }
        if(!curYear)
        {
            format2 += ", yyyy";
        }

        SimpleDateFormat sdf1 = new SimpleDateFormat(format);
        SimpleDateFormat sdf2 = new SimpleDateFormat(format2);
        return sdf1.format(d1) + middle + sdf2.format(d2);
    }

    //To handle multiple scrollviews
    protected class TouchListener implements ListView.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Disallow ScrollView to intercept touch events.
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;

                case MotionEvent.ACTION_UP:
                    // Allow ScrollView to intercept touch events.
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }

            // Handle ListView touch events.
            v.onTouchEvent(event);
            return true;
        }
    }

    class GetEventTask extends AsyncTask<Void, Void, String> {

        ProgressDialog progressDialog;
        Activity response;
        String sEventID, creator, sEventDetails = "";
        Attendee attendees;

        public GetEventTask (String s)
        {
            sEventID = s;
        }

        @Override
        protected void onPreExecute()
        {
            progressDialog= ProgressDialog.show(ViewEventActivity.this, getString(R.string.view_event_progress_bar_title) + "...", getString(R.string.progress_dialog_text), true);
        }

        @Override
        protected String doInBackground(Void... params) {

            ActsApi getEventApi = new ActsApi();
            getEventApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            getEventApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);

            try {
                response = getEventApi.getActivity(sEventID);
                sEventDetails = response.toString();
                attendees = getEventApi.getAttendees(sEventID, null);
                creator = response.getCreator();
            } catch (Exception e) {
                sEventDetails = e.getMessage();
                Log.e("Error - Get Activity", sEventDetails);
            }

            UsersApi usersApi = new UsersApi();
            usersApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            usersApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);
            UserProfile sUserProfile;

            if(creator.equalsIgnoreCase(PrefUtil.getString(getApplicationContext(), IAppConstants.USER_ID, null)))
            {
                mIsACreator = true;
            }
            // to handle bad events
            try
            {
                for(int i=0; i < attendees.getJoiners().size(); i++)
                {
                    try {
                        Joiner joiner = attendees.getJoiners().get(i);
                        //check if a user is a joiner of this event
                        if(joiner.getJoiner_id().toString().equalsIgnoreCase(PrefUtil.getString(getApplicationContext(), IAppConstants.USER_ID, null)))
                        {
                            mIsAJoiner = true;
                        }
                        sUserProfile = usersApi.getUserProfile(joiner.getJoiner_id().toString());
                        mAttendees.add(sUserProfile);
                    } catch (Exception e) {
                        sEventDetails = e.getMessage();
                        Log.e("Error - User Profile", sEventDetails);
                    }
                }
            }
            catch(NullPointerException e)
            {
                sEventDetails = e.getMessage();
                Log.e("Error - Get Joiners", sEventDetails);
            }

            return sEventDetails;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            progressDialog.dismiss();

            //handle badly created events
            if(result.contains("error"))
            {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            }
            else
            {
                setTitle(response.getName());
                String desc = response.getDescription();
                if(desc.length() == 0)
                {
                    mEventDesc.setVisibility(View.GONE);
                }
                else
                {
                    mEventDesc.setText(desc);
                }

                if(mIsACreator)
                {
                    mJoinEventBtn.setText(getString(R.string.view_event_joiner_button));
                    mCancelBtn.setText("Cancel Event");
                }
                else if(mIsAJoiner)
                {
                    mJoinEventBtn.setText(getString(R.string.view_event_joiner_button));
                    mCancelBtn.setText("Un-Join");
                }

                //Hide the suggest buttons if the creator does not allow user input
                //and if the user is not a joiner
                if(!response.getAllow_joiner_input() && !mIsACreator)
                {
                    mEventLocation.setVisibility(View.GONE);
                    mSuggestTimeBtn.setVisibility(View.GONE);
                    mAddLocationBtn.setVisibility(View.GONE);
                }

                addAttendees();
                addLocation(response.getLocations());
                addTime(response.getTimes());
            }
        }

        private void addAttendees()
        {
            //TODO: (Jobelle) View Event - Attendees Profile Pic
            for(int i=0; i < mAttendees.size(); i++) {
                View joiner = getLayoutInflater().inflate(R.layout.joiners_layout, mAttendeesList, false);
                LinearLayout joinersList = (LinearLayout) joiner.findViewById(R.id.joiner_layout);
                TextView joinerName = (TextView) joinersList.findViewById(R.id.joiner_name);
                joinerName.setText(mAttendees.get(i).getUsername());
                final UserProfile profile = mAttendees.get(i);

                //add on click listener
                joinersList.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getBaseContext(), ViewProfileActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                        intent.putExtra("userName", profile.getUsername()); //add in the user name
                        intent.putExtra("userProfile", profile.getProfile_pic());
                        intent.putExtra("userId", profile.getUser_id());
                        intent.putExtra("favourited", profile.getFavourited());
                        intent.putStringArrayListExtra("userInterest", (ArrayList<String>)profile.getTagged_interests());
                        startActivity(intent);
                    }
                });

                mAttendeesList.addView(joinersList);
            }
        }

        private void addLocation(List<Location> suggestedLocs)
        {
            Address addr;
            List<Address> addressList;
            int numOfVotes;

            for (int i = 0; i < suggestedLocs.size(); i++)
            {
                Location suggestion = suggestedLocs.get(i);
                try {
                    Geocoder geocoder = new Geocoder(getBaseContext(), Locale.CANADA);
                    addressList = geocoder.getFromLocation(suggestion.getLatitude(),
                            suggestion.getLongitude(), 1);

                    if (addressList != null && addressList.size() > 0) {
                        addr = addressList.get(0);
                        String addressText = (addr.getMaxAddressLineIndex() > 0 ? addr.getAddressLine(0) : "")+" "+
                                                (addr.getLocality() != null ? addr.getLocality() : "")+" "+
                                                (addr.getCountryName() != null ? addr.getCountryName() : "");

                        //add votes
                        try {
                            numOfVotes = suggestion.getSuggestion_votes().intValue();
                        } catch(NullPointerException e)
                        {
                            numOfVotes = -1;
                        }

                        boolean yesVote = suggestion.getSuggestion_voters().contains(
                                PrefUtil.getString(getApplicationContext(), IAppConstants.USER_ID, null));
                        locSuggestionList.add(new SuggestionListItem(suggestion.getSuggestion_id(),
                                addressText, numOfVotes, yesVote));
                    }
                } catch (IOException e) {
                    Log.e("View Event", e.getMessage());
                }
            }

            mLocationListAdapter = new SuggestionListAdapter(ViewEventActivity.this, locSuggestionList);
            mLocationList.setAdapter(mLocationListAdapter);
            ListViewUtil.setListViewHeightBasedOnChildren(mLocationList);
        }

        private void addTime(List<Time> times)
        {

            for (int i = 0; i < times.size(); i++) {
                Time time = times.get(i);

                int numOfVotes;
                //add votes
                try {
                    numOfVotes = time.getSuggestion_votes().intValue();
                } catch(NullPointerException e)
                {
                    numOfVotes = -1;
                }

                boolean yesVote = time.getSuggestion_voters().contains(
                        PrefUtil.getString(getApplicationContext(), IAppConstants.USER_ID, null));
                timeSuggestionList.add(new SuggestionListItem(time.getSuggestion_id(),
                        convertToTimeToString(time), numOfVotes, yesVote));
            }

            mTimesListAdapter = new SuggestionListAdapter(ViewEventActivity.this, timeSuggestionList);
            mTimeList.setAdapter(mTimesListAdapter);
            ListViewUtil.setListViewHeightBasedOnChildren(mTimeList);
        }

    }

    class JoinEventTask extends AsyncTask<Void, Void, String>
    {
        String sEventID, response;
        ProgressDialog progressDialog;
        ActsApi getEventApi;

        public JoinEventTask (String s)
        {
            sEventID = s;
        }

        @Override
        protected void onPreExecute()
        {
            progressDialog= ProgressDialog.show(ViewEventActivity.this, getString(R.string.join_event_progress_bar_title) + "...", getString(R.string.progress_dialog_text), true);
        }

        @Override
        protected String doInBackground(Void... params)
        {
            getEventApi = new ActsApi();
            getEventApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            getEventApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);

            try {
                getEventApi.joinActivity(sEventID);
                checkPendingSuggestions();
                checkVotes();
            } catch (Exception e) {
                response = e.getMessage();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String message) {
            progressDialog.dismiss();
            if(mIsAJoiner)
            {
                message = "Event updated";
            }
            else
            {
                message = "Joined Event!";
            }

            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            finish();
        }

        private void checkPendingSuggestions()
        {
            int pendingLocCount = mSuggestedLocList.getChildCount();
            int pendingTimeCount = mSuggestedTimeList.getChildCount();

            for(int i = 0; i < pendingLocCount; i++)
            {
                try
                {
                    getEventApi.suggestLocationForActivity(sEventID, mPendingLocSuggestionAdapter.getItem(i).getLoc());
                }
                catch (Exception e) {
                    response = e.getMessage();
                }
            }

            for(int i = 0; i < pendingTimeCount; i++)
            {
                try
                {
                    getEventApi.suggestTimeForActivity(sEventID, mPendingTimeSuggestionAdapter.getItem(i).getTime());
                }
                catch (Exception e) {
                    response = e.getMessage();
                }
            }
        }

        private void checkVotes()
        {

            int addrCount = mLocationList.getChildCount();
            int timeCount = mTimeList.getChildCount();

            for (int i=0; i<addrCount; i++)
            {
                if(((CheckBox)mLocationList.getChildAt(i)
                        .findViewById(R.id.suggestion_checkbox)).isChecked())
                {
                    try {
                        getEventApi.voteForSuggestion(mLocationListAdapter.getItem(i).getSuggestionId());
                    } catch (Exception e) {
                        response = e.getMessage();
                    }
                }
                else
                {
                    try {
                        getEventApi.unvoteForSuggestion(mLocationListAdapter.getItem(i).getSuggestionId());
                    } catch (Exception e) {
                        response = e.getMessage();
                    }
                }
            }

            for (int i=0; i<timeCount; i++)
            {
                if(((CheckBox)mTimeList.getChildAt(i)
                        .findViewById(R.id.suggestion_checkbox)).isChecked())
                {
                    try {
                        getEventApi.voteForSuggestion(mTimesListAdapter.getItem(i).getSuggestionId());
                    } catch (Exception e) {
                        response = e.getMessage();
                    }
                }
                else
                {
                    try {
                        getEventApi.unvoteForSuggestion(mTimesListAdapter.getItem(i).getSuggestionId());
                    } catch (Exception e) {
                        response = e.getMessage();
                    }
                }
            }
        }
    }

    class UnJoinEventTask extends AsyncTask<Void, Void, String>
    {
        String sEventID, response;
        ProgressDialog progressDialog;
        ActsApi getEventApi;

        public UnJoinEventTask (String s)
        {
            sEventID = s;
        }

        @Override
        protected void onPreExecute()
        {
            progressDialog = ProgressDialog.show(ViewEventActivity.this,
                    getString(R.string.unjoin_event_progress_bar_title) + "...",
                    getString(R.string.progress_dialog_text), true);
        }

        @Override
        protected String doInBackground(Void... params)
        {
            getEventApi = new ActsApi();
            getEventApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            getEventApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);

            try {
                getEventApi.unjoinActivity(sEventID);
            } catch (Exception e) {
                response = e.getMessage();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String message) {
            progressDialog.dismiss();
            message = "Un-Joined Event";
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    class CancelEventTask extends AsyncTask<Void, Void, String>
    {
        String sEventID, response;
        ProgressDialog progressDialog;
        ActsApi getEventApi;

        public CancelEventTask (String s)
        {
            sEventID = s;
        }

        @Override
        protected void onPreExecute()
        {
            progressDialog = ProgressDialog.show(ViewEventActivity.this,
                    getString(R.string.cancel_event_progress_bar_title) + "...",
                    getString(R.string.progress_dialog_text), true);
        }

        @Override
        protected String doInBackground(Void... params)
        {
            getEventApi = new ActsApi();
            getEventApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            getEventApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);

            try {
                getEventApi.cancelActivity(sEventID);
            } catch (Exception e) {
                response = e.getMessage();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String message) {
            progressDialog.dismiss();
            message = "Cancelled Event";
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    class AddSuggestionTask extends AsyncTask<Void, Void, String>
    {
        ActsApi setEventApi;
        String sEventID = eventID, response = "";
        Time suggestedTime = null;
        Location suggestedLocation = null;
        String address;
        boolean isTime;
        CreateSuggestionResponse suggested;

        public AddSuggestionTask(Time t)
        {
            suggestedTime = t;
            isTime = true;
        }

        public AddSuggestionTask(Location loc, String s)
        {
            suggestedLocation = loc;
            address = s;
            isTime = false;
        }

        @Override
        protected String doInBackground(Void... params)
        {
            setEventApi = new ActsApi();
            setEventApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            setEventApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);

            try {

                if(isTime)
                {
                    suggested = setEventApi.suggestTimeForActivity(sEventID, suggestedTime);
                }
                else
                {
                    suggested = setEventApi.suggestLocationForActivity(sEventID, suggestedLocation);
                }



            } catch (Exception e) {
                response = e.getMessage();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String message) {
            Log.e("Suggestion", message);

            if(isTime)
            {
                timeSuggestionList.add(new SuggestionListItem(suggested.getSuggestion_id(),
                        convertToTimeToString(suggestedTime), 1, true));
                mTimesListAdapter.notifyDataSetChanged();
                ListViewUtil.setListViewHeightBasedOnChildren(mTimeList);
            }
            else
            {
                locSuggestionList.add(new SuggestionListItem(suggested.getSuggestion_id(),
                        address, 1, true));
                mLocationListAdapter.notifyDataSetChanged();
                ListViewUtil.setListViewHeightBasedOnChildren(mLocationList);
            }

        }
    }
}
