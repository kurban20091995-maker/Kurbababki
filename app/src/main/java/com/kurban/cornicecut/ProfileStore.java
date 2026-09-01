package com.kurban.cornicecut;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public final class ProfileStore {
    private static final String PREF = "cornice_profiles_v2";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_LEFTOVERS_PREFIX = "leftovers_";

    private final SharedPreferences prefs;

    public ProfileStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static final class Profile {
        public String id;
        public String name;
        public double height;
        public double profileOverhang;
        public double platform;
        public double effectiveOverhang;
        public double springAngle;
        public String stockSpec;
        public String color;
        public String coating;
        public String batch;

        public Profile copy() {
            Profile p = new Profile();
            p.id = id; p.name = name; p.height = height; p.profileOverhang = profileOverhang;
            p.platform = platform; p.effectiveOverhang = effectiveOverhang; p.springAngle = springAngle;
            p.stockSpec = stockSpec; p.color = color; p.coating = coating; p.batch = batch;
            return p;
        }

        @Override public String toString() { return name; }
    }

    public List<Profile> loadProfiles() {
        String raw = prefs.getString(KEY_PROFILES, "");
        List<Profile> out = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] rows = raw.split("\\n");
            for (String row : rows) {
                String[] f = row.split("\\t", -1);
                if (f.length < 10) continue;
                try {
                    Profile p = new Profile();
                    p.id = dec(f[0]);
                    p.name = dec(f[1]);
                    p.height = Double.parseDouble(f[2]);
                    p.profileOverhang = Double.parseDouble(f[3]);
                    p.platform = Double.parseDouble(f[4]);
                    p.effectiveOverhang = Double.parseDouble(f[5]);
                    p.springAngle = Double.parseDouble(f[6]);
                    p.stockSpec = dec(f[7]);
                    p.color = dec(f[8]);
                    p.coating = dec(f[9]);
                    p.batch = f.length > 10 ? dec(f[10]) : "";
                    out.add(p);
                } catch (Exception ignored) {}
            }
        }
        if (out.isEmpty()) {
            Profile p = new Profile();
            p.id = "default";
            p.name = "Мой карниз";
            p.height = 80;
            p.profileOverhang = 60;
            p.platform = 50;
            p.effectiveOverhang = 60;
            p.springAngle = 38;
            p.stockSpec = "2700x3";
            p.color = "";
            p.coating = "";
            p.batch = "";
            out.add(p);
        }
        return out;
    }

    public void saveProfiles(List<Profile> profiles) {
        StringBuilder sb = new StringBuilder();
        for (Profile p : profiles) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(enc(nz(p.id))).append('\t')
              .append(enc(nz(p.name))).append('\t')
              .append(p.height).append('\t')
              .append(p.profileOverhang).append('\t')
              .append(p.platform).append('\t')
              .append(p.effectiveOverhang).append('\t')
              .append(p.springAngle).append('\t')
              .append(enc(nz(p.stockSpec))).append('\t')
              .append(enc(nz(p.color))).append('\t')
              .append(enc(nz(p.coating))).append('\t')
              .append(enc(nz(p.batch)));
        }
        prefs.edit().putString(KEY_PROFILES, sb.toString()).apply();
    }

    public String loadLeftovers(String profileId) {
        return prefs.getString(KEY_LEFTOVERS_PREFIX + profileId, "");
    }

    public void saveLeftovers(String profileId, String spec) {
        prefs.edit().putString(KEY_LEFTOVERS_PREFIX + profileId, spec == null ? "" : spec.trim()).apply();
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String enc(String s) { return Uri.encode(s); }
    private static String dec(String s) { return Uri.decode(s); }
}
