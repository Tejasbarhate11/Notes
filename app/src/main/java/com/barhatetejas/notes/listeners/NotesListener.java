package com.barhatetejas.notes.listeners;

import com.barhatetejas.notes.entities.Note;

public interface NotesListener {

    void onNoteClicked(Note note, int position);

}
