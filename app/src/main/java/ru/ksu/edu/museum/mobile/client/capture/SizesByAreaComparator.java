package ru.ksu.edu.museum.mobile.client.capture;

import android.util.Size;

import java.util.Comparator;

public class SizesByAreaComparator implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
        return Long.signum((long) lhs.getWidth() * lhs.getHeight()
                - (long) rhs.getWidth() * rhs.getHeight());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        return o instanceof Comparator;
    }
}
