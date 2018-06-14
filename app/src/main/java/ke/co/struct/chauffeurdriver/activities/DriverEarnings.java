package ke.co.struct.chauffeurdriver.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ke.co.struct.chauffeurdriver.R;

public class DriverEarnings extends  AppCompatActivity {
    private TextView txtEarnings,txtTrips;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String userId;
    private Double amount = 0.0;
    private int trips = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_earnings);
        userId = mAuth.getCurrentUser().getUid();
        txtEarnings = findViewById(R.id.earnings);
        txtTrips = findViewById(R.id.txtTrips);
        DatabaseReference earningsRef = database.getReference().child("driverEarnings").child(userId).child("earnings");
        earningsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    amount = Double.parseDouble(dataSnapshot.getValue().toString());
                    txtEarnings.setText(Double.toString(amount));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        earningsRef.keepSynced(true);
        DatabaseReference tripsRef = database.getReference().child("driverTrips").child(userId).child("trips");
        tripsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    trips = Integer.parseInt(dataSnapshot.getValue().toString());
                    txtTrips.setText(Integer.toString(trips));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
