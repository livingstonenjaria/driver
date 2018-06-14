package ke.co.struct.chauffeurdriver.activities;

import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.remote.Common;

public class PaymentActivity extends AppCompatActivity {
    private String tripDistance,tripCost,content,pushid,userid;
    private TextView totalPrice, totalDistance;
    private Button confirm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        totalDistance =findViewById(R.id.txtDistance);
        totalPrice =findViewById(R.id.rideCost);
        confirm = findViewById(R.id.confirmPayment);
        userid = Common.auth.getCurrentUser().getUid();
        if (getIntent() != null){
            content = getIntent().getStringExtra("content");
            pushid = getIntent().getStringExtra("pushid");
            Double total_distance = Math.ceil(Common.trip);
            tripDistance = total_distance.toString();
            tripCost = content.toString();
            totalDistance.setText(total_distance.toString());
            totalPrice.setText(content);
            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    paymentConfirmation();
                }
            });
        }
    }
    /*-----------------Confirm Payment--------------------------------*/
    private void paymentConfirmation() {
        DatabaseReference confirmPayment = Common.database.getReference().child("paymentConfirmation");
        String pushId = confirmPayment.push().getKey();
        HashMap map = new HashMap();
        map.put("driver",userid);
        map.put("pushid",pushId);
        map.put("distance",tripDistance);
        map.put("cost",tripCost);
        confirmPayment.child(pushId).updateChildren(map);


    }
}
