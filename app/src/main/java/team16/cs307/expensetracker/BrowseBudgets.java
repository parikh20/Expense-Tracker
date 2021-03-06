package team16.cs307.expensetracker;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;


import android.content.Context;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.round;

public class BrowseBudgets extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private TextView mName;
    private TextView mMonthly;
    private TextView mWeekly;
    private TextView mYearly;
    private TextView mLimits;
    private TextView mRatingView;
    private Button mFinish;

    private ArrayList<Limit> limits;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Budget curr_budg;
    private Spinner mBudgets;
    private Spinner mRating;
    private String i;
    private String currDocName="";
    private int numRating = 0;
    private int totalrating = 0;
    private int userRating = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_browse_budgets);
        mName = findViewById(R.id.browse_budgets_name);
        mMonthly = findViewById(R.id.browse_budget_monthly);
        mWeekly = findViewById(R.id.browse_budget_weekly);
        mYearly = findViewById(R.id.browse_budget_yearly);
        mLimits = findViewById(R.id.browse_budget_current_limits);
        mFinish = findViewById(R.id.browse_budget_select);
        mBudgets = findViewById(R.id.browse_budget_selector);
        mBudgets.setOnItemSelectedListener(this);
        mRating = findViewById(R.id.budget_rating);
        mRating.setOnItemSelectedListener(this);
        mRatingView = findViewById(R.id.browse_rating);


        limits = new ArrayList<Limit>();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        //populate public budget spinner from db
        CollectionReference refE = db.collection("PublicBudgets");
        refE.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot querySnapshot) {

                int x = 0;
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    x++;
                }

                ArrayList<Budget> bi = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {

                    Budget b = doc.toObject(Budget.class);
                    bi.add(b);

                }
                String[] arraySpinner = new String[x + 1];
                arraySpinner[0] = "Choose From Public Budgets!";
                int itr = 1;
                for (Budget b : bi) {

                    arraySpinner[itr] = b.getName();
                    itr++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arraySpinner);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mBudgets.setAdapter(adapter);
            }
        });

        // Populate rating spinner
        String[] intSpinner = new String[6];
        intSpinner[0] = "Choose a Rating!";
        intSpinner[1] = "1";
        intSpinner[2] = "2";
        intSpinner[3] = "3";
        intSpinner[4] = "4";
        intSpinner[5] = "5";

        ArrayAdapter<String> ratingsAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, intSpinner);
        ratingsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRating.setAdapter(ratingsAdapter);

        mFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (curr_budg == null) {
                    return;
                }

                db.collection("users").document(mAuth.getUid()).collection("Preferences").document("Current Budget").set(curr_budg);
                db.collection("PublicBudgets").document(currDocName).update("numRatings",numRating+1);
                db.collection("PublicBudgets").document(currDocName).update("totalRating",totalrating+userRating);
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);

                finish();
            }
        });

        mRating.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                if (i != null) {
                    System.out.println(i);

////                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PublicBudgets").child(i);
//
//                    Map<String, Object> update = new HashMap<String, Object>();
//                    update.put("totalRating", 5);
//                    ref.updateChildren(update);
                    final DocumentReference dRef = db.collection("PublicBudgets").document(currDocName);
                    dRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                userRating = position;
                                DocumentSnapshot document = task.getResult();
                                numRating = document.getLong("numRatings").intValue();
                                totalrating = document.getLong("totalRating").intValue();


                            }
                        }
                    });

                }
                //Toast.makeText(BrowseBudgets.this,Integer.toString(position),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        }); {

        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        String item = parent.getItemAtPosition(position).toString();
        i = item;
        System.out.println("=========================================================");
        System.out.println(item);
        if (item == "Choose From Public Budgets") {
            return;
        }

        //make all else visible and populate it with retrieved values
        DocumentReference ref = db.collection("PublicBudgets").document(item);
        currDocName=item;
        ref.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                curr_budg = documentSnapshot.toObject(Budget.class);
                if (curr_budg != null) {
                    limits = new ArrayList<Limit>();
                    String str = "Limits: \n";
                    boolean first = true;
                    for (Limit lim : curr_budg.getCustomLimits()) {
                        limits.add(lim);
                        if (!first) {
                            str += (", \n");
                        }
                        str += (lim.getCategory() + ": Monthly Limit: " + lim.getLimitMonthly() );
                        first = false;
                    }

                    mLimits.setText(str);
                    String month = "Monthly Limit: " + String.valueOf(curr_budg.getLimitMonthly());
                    String week = "Weekly Limit: " + String.valueOf(curr_budg.getLimitWeekly());
                    String year = "Yearly Limit: " + String.valueOf(curr_budg.getLimitYearly());
                    double overallRating = Double.valueOf(curr_budg.getTotalRating())/Double.valueOf(curr_budg.getNumRatings());
                    mRatingView .setText(String.format("%1.1f",overallRating)+"/"+"5"+"  ("+curr_budg.getNumRatings()+"  reviews)");
                    mMonthly.setText(month);
                    mWeekly.setText(week);
                    mYearly.setText(year);
                    String name = "Name: " + curr_budg.getName();
                    mName.setText(name);
                }
            }
        });
}

    public void onNothingSelected(AdapterView<?> arg0) {
        //do nothing
    }


}

