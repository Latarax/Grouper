package android.grouper.broTeam;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateNewTask extends AppCompatActivity {

    String mGroupid;
    EditText titleText, descriptionText, locationText;
    Button cancelBtn, createBtn;
    Spinner assignedTo;
    List<String> groupMembers = new ArrayList<>();

    // place stuff
    private String placeName;
    private LatLng placeLatLng;
    private double placeLat;
    private double placeLng;
    private String placeID;
    int AUTOCOMPLETE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group_task);

        Intent intent = getIntent();
        mGroupid = intent.getStringExtra("iGroupId");

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        titleText = findViewById(R.id.createTaskTitle);
        descriptionText = findViewById(R.id.createTaskDescription);
        locationText = findViewById(R.id.taskCreateAddress);
        assignedTo = findViewById(R.id.assignCreateUser);

        getGroupMembers();

        locationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set the fields to specify which types of place data to
                // return after the user has made a selection.
                List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

                // Start the autocomplete intent.
                Intent autoPlaceIntent = new Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.OVERLAY, fields).build(CreateNewTask.this);
                startActivityForResult(autoPlaceIntent, AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        cancelBtn = findViewById(R.id.cancelTaskCreate);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goToGroupTaskDisplay = new Intent(CreateNewTask.this, GroupTaskDisplay.class );
                goToGroupTaskDisplay.putExtra("iGroupId", mGroupid);
                startActivity(goToGroupTaskDisplay);
                finish();
            }
        });

        createBtn = findViewById(R.id.createTaskButton);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveTask();
            }
        });

        getSupportActionBar().setTitle("Create New Task");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Result Code", ""+resultCode);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                placeName = place.getName();
                placeLatLng = place.getLatLng();
                placeLat = placeLatLng.latitude;
                placeLng = placeLatLng.longitude;
                placeID = place.getId();
                locationText.setText(placeName);

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.d("Status Message", status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    private void getGroupMembers() {

        groupMembers.add("Unassigned");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference groupReference = database.collection("groupsList").document(mGroupid);
        groupReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String, Map<String, Object>> membersMap = (Map<String, Map<String, Object>>) documentSnapshot.get("Members");

                for (int i = 0; i < membersMap.size(); i++) {

                    Map<String, Object> member = membersMap.get("" + i);

                    final DocumentReference groupMember = (DocumentReference) member.get("Member");
                    groupMember.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {

                            if(!groupMembers.contains(documentSnapshot.getString("Username"))) {
                                groupMembers.add(documentSnapshot.getString("Username"));
                            }
                        }
                    });
                }
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(CreateNewTask.this,
                        android.R.layout.simple_spinner_item, groupMembers);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                assignedTo.setAdapter(dataAdapter);
            }
        });
    }

    private void saveTask() {
        final String taskName = titleText.getText().toString().trim();
        final String taskDescribe = descriptionText.getText().toString().trim();

        if (taskName.isEmpty()) {
            titleText.setError("Must enter a Task Title");
            titleText.requestFocus();
            return;
        } else if (taskDescribe.isEmpty()){
            descriptionText.setError("Must enter a Task Description");
            descriptionText.requestFocus();
            return;
        }
        if(placeName == null){
            placeName = "";
            placeID = "";
            placeLat = 0;
            placeLng = 0;
        }

        final FirebaseFirestore database = FirebaseFirestore.getInstance();
        final CollectionReference taskCollection = database.collection("groupsList").document(mGroupid)
                .collection("tasks");

        String assignedUsername = assignedTo.getSelectedItem().toString();
        database.collection("usersList").whereEqualTo("Username", assignedUsername)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<DocumentSnapshot> documentSnapshotList = queryDocumentSnapshots.getDocuments();
                DocumentSnapshot user = documentSnapshotList.get(0);

                Map<String, Object> taskData = new HashMap<>();
                taskData.put("assignedUser", database.collection("usersList").document(user.getId()));
                taskData.put("description", taskDescribe);
                taskData.put("placeName", placeName);
                taskData.put("placeID", placeID);
                taskData.put("placeLat", placeLat);
                taskData.put("placeLng", placeLng);
                taskData.put("taskName", taskName);
                taskData.put("isDone", false);

                taskCollection.add(taskData);

                Toast.makeText(getApplicationContext(), "Task Updated", Toast.LENGTH_SHORT).show();
                Intent goToGroupTaskDisplay = new Intent(CreateNewTask.this, GroupTaskDisplay.class );
                goToGroupTaskDisplay.putExtra("iGroupId", mGroupid);
                startActivity(goToGroupTaskDisplay);
                finish();
            }
        });
    }
}
