package com.laodev.chatapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.ContactsAdapter;
import com.laodev.chatapp.models.Attachment;
import com.laodev.chatapp.models.DownloadFileEvent;
import com.laodev.chatapp.utils.Helper;
import com.squareup.picasso.Picasso;

import java.io.File;

import ezvcard.Ezvcard;
import ezvcard.VCard;

public class ContactViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_v_card_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.contact);
        }

        ImageView contactImage = findViewById(R.id.contactImage);
        TextView contactName = findViewById(R.id.contactName);
        RecyclerView contactPhones = findViewById(R.id.recyclerPhone);
        RecyclerView contactEmails = findViewById(R.id.recyclerEmail);

        contactPhones.setLayoutManager(new LinearLayoutManager(this));
        contactEmails.setLayoutManager(new LinearLayoutManager(this));

        final Attachment attachment = getIntent().getParcelableExtra("data");
        final boolean isMine = getIntent().getBooleanExtra("what", false);
        final VCard vcard = Ezvcard.parse(attachment.getData()).first();

        View view = findViewById(R.id.contactAdd);
        view.setOnClickListener(v -> {
            File file = new File(Environment.getExternalStorageDirectory() + "/"
                    + getString(R.string.app_name) + "/" + "Contact" + (isMine ? "/.sent/" : "")
                    , attachment.getName());
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(ContactViewerActivity.this,
                        getString(R.string.authority),
                        file);
                intent.setDataAndType(uri, Helper.getMimeType(ContactViewerActivity.this, uri)); //storage path is path of your vcf file and vFile is name of that file.
                startActivity(intent);
            } else if (!isMine) {
                Intent intent = new Intent(Helper.BROADCAST_DOWNLOAD_EVENT);
                intent.putExtra("data", new DownloadFileEvent(0, attachment, -1));
                LocalBroadcastManager.getInstance(ContactViewerActivity.this).sendBroadcast(intent);
            } else {
                Toast.makeText(ContactViewerActivity.this, "File unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        if (vcard.getPhotos().size() > 0)
            Picasso.get()
                    .load(String.valueOf(vcard.getPhotos().get(0).getData()))
                    .tag(this)
                    .placeholder(R.drawable.ic_avatar)
                    .into(contactImage);

        contactName.setText(vcard.getFormattedName().getValue());

        contactPhones.setAdapter(new ContactsAdapter(this, vcard.getTelephoneNumbers(), null));
        contactEmails.setAdapter(new ContactsAdapter(this, null, vcard.getEmails()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return (super.onOptionsItemSelected(menuItem));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
