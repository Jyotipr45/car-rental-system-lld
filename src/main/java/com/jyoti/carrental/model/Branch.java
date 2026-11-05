package com.jyoti.carrental.model;

/**
 * A physical rental location. Vehicles belong to a branch and are picked up there.
 *
 * <p>Branches are data (admins add them at runtime), so they are an entity, not an enum.</p>
 *
 * <p>Relationship: Branch 1 ──── N Vehicle. The foreign key ({@code branchId}) lives on
 * the "many" side, i.e. in {@link Vehicle}.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class Branch extends BaseEntity {

    private String name;
    private String address;
    private String city;

    public Branch(String name, String address, String city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }

    @Override
    public String toString() {
        return "Branch{id=" + getId() + ", name='" + name + "', city='" + city + "'}";
    }
}
