package com.andruid.magic.objectdetection;

import android.graphics.Bitmap;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

public interface Classifier {
    List<Recognition> recognizeImage(Bitmap bitmap);

    void close();

    Vector<String> getLabels();

    void addSelectedLabel(String label);

    void addSelectedLabels(Collection<String> labels);

    Vector<String> getSelectedLabels();

    void clearSelected();

    void removeLabel(int index);

    void removeLabel(String label);

    void removeLabels(Collection<String> labels);

    int getMode();

    void setMode(int mode);
}