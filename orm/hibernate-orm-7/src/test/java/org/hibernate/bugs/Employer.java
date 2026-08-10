package org.hibernate.bugs;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Employer implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private int version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "employer")
    private final List<EmploymentLink> employmentLinks = new ArrayList<>();

    public List<Person> getPersons() {
        return this.employmentLinks.stream()
                .map(EmploymentLink::getPerson)
                .toList();
    }

    public Long getId() {
        return id;
    }
}
