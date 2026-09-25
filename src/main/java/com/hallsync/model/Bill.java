package com.hallsync.model;

public record Bill(String month, double rent, double meals, double electricity, double paid) {
    public double total() { return rent + meals + electricity; }
    public double due() { return Math.max(0, total() - paid); }
}
