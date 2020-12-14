package com.barhatetejas.notes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.barhatetejas.notes.R;
import com.barhatetejas.notes.database.NotesDatabase;
import com.barhatetejas.notes.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime, textURL;
    private View viewSubtitleIndicator;
    private ImageView imageBack, imageSave, imageNote, imageRemoveImage, imageRemoveURL;
    private LinearLayout layoutURL;

    private AlertDialog dialogAddURL, dialogDeleteNote;

    private String selectedNoteColor;
    private String selectedImagePath;

    private Note alreadyAvailableNote;

    //REQUEST CODES
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        //Binding Views
        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imageBack = findViewById(R.id.imageBack);
        imageSave = findViewById(R.id.imageSave);
        imageNote = findViewById(R.id.imageNote);
        imageRemoveImage = findViewById(R.id.imageRemoveImage);
        imageRemoveURL = findViewById(R.id.imageRemoveURL);
        textURL = findViewById(R.id.textURL);
        layoutURL = findViewById(R.id.layoutURL);

        //Variable initialization
        selectedNoteColor = "#FFC000";
        selectedImagePath = "";

        //Back btn
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });



        //set the date and time
        textDateTime.setText(
                new SimpleDateFormat("E, dd MMM yyyy hh:mm a", Locale.getDefault())
                .format(new Date())
        );


        imageSave.setOnClickListener(view -> saveNote());
        imageRemoveImage.setOnClickListener(view -> {
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            imageRemoveImage.setVisibility(View.GONE);
            selectedImagePath = "";
        });
        imageRemoveURL.setOnClickListener(view -> {
            textURL.setText("");
            layoutURL.setVisibility(View.GONE);

        });

        if (getIntent().getBooleanExtra("isViewOrUpdate",false)){
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        if (getIntent().getBooleanExtra("isFromQuickAction", false)) {
            String type = getIntent().getStringExtra("quickActionType");
            if (type != null) {
                if (type.equals("image")) {
                    selectedImagePath = getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                } else if (type.equals("URL")) {
                    textURL.setText(getIntent().getStringExtra("URL"));
                    layoutURL.setVisibility(View.VISIBLE);
                }
            }
        }



        initMiscellaneous();
        setSubtitleIndicatorColor();

    }

    private void setViewOrUpdateNote() {
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            textURL.setText(alreadyAvailableNote.getWebLink());
            layoutURL.setVisibility(View.VISIBLE);
        }
    }


    //Function to save the Note.
    private void saveNote(){
        //Validation check
        if (inputNoteTitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Please enter a title.", Toast.LENGTH_SHORT).show();
            return;
        }else if (inputNoteSubtitle.getText().toString().trim().isEmpty()
        && inputNoteText.getText().toString().isEmpty()){
            Toast.makeText(this, "Note can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }


        final Note note = new Note();

        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        //To update the note
        if (alreadyAvailableNote != null){
            note.setId(alreadyAvailableNote.getId());;
        }

        //Async Task to save the note to the db.
        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void,Void,Void>{
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }

        new SaveNoteTask().execute();
    }

    //Function to initiate the Miscellaneous tab
    private void initMiscellaneous(){

        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);
        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }else{
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        //The color picker
        final ImageView imageColor1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layoutMiscellaneous.findViewById(R.id.imageColor5);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(view -> {
            selectedNoteColor = "#FFC000";
            imageColor1.setImageResource(R.drawable.ic_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });
        layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(view -> {
            selectedNoteColor = "#BABABA";
            imageColor2.setImageResource(R.drawable.ic_done);
            imageColor1.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });
        layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(view -> {
            selectedNoteColor = "#F38C49";
            imageColor3.setImageResource(R.drawable.ic_done);
            imageColor2.setImageResource(0);
            imageColor1.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });
        layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(view -> {
            selectedNoteColor = "#66A9E0";
            imageColor4.setImageResource(R.drawable.ic_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor1.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });
        layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(view -> {
            selectedNoteColor = "#64DD17";
            imageColor5.setImageResource(R.drawable.ic_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor1.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#BABABA":
                    layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
                    break;

                case "#F38C49":
                    layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
                    break;

                case "#66A9E0":
                    layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
                    break;

                case "#64DD17":
                    layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
                    break;

            }
        }

        //Image adding
        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(CreateNoteActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
            }else {
                selectImage();
            }
        });

        //URL adding
        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddUrlDialog();
        });

        //To make the delete option visible if the note is already created
        if (alreadyAvailableNote != null) {
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDeleteNoteDialog();
                }
            });
        }
    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note,findViewById(R.id.layoutDeleteNoteContainer));
            builder.setView(view);


            dialogDeleteNote = builder.create();
            dialogDeleteNote.setCanceledOnTouchOutside(true);

            if (dialogDeleteNote.getWindow() !=null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            //Buttons
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    class DeleteNoteTask extends AsyncTask<Void,Void, Void>{

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Intent intent = new Intent();
                            intent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK,intent);
                            finish();
                        }
                    }

                    new DeleteNoteTask().execute();
                }
            });
            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogDeleteNote.dismiss();
                }
            });

        }
        dialogDeleteNote.show();
    }

    private void showAddUrlDialog() {
        if (dialogAddURL == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url,(ViewGroup) findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);

            dialogAddURL = builder.create();

            if (dialogAddURL.getWindow() != null){
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (inputURL.getText().toString().trim().isEmpty()) {
                        Toast.makeText(CreateNoteActivity.this, "URL field empty.", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(CreateNoteActivity.this, "Please enter a valid URL link", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    textURL.setText(inputURL.getText().toString());
                    layoutURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }


    //Function to select image from gallery
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    //Function to get The Path to store in the db
    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    //Function to apply the picked color.
    private void setSubtitleIndicatorColor(){
        GradientDrawable gradientDrawable  = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
     }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else{
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if (data != null){
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null){
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap map = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(map);
                        imageNote.setVisibility(View.VISIBLE);
                        selectedImagePath = getPathFromUri(selectedImageUri);
                        imageRemoveImage.setVisibility(View.VISIBLE);

                    } catch (FileNotFoundException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "Cancelled!", Toast.LENGTH_SHORT).show();
        }
    }
}