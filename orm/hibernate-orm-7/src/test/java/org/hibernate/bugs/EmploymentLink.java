package org.hibernate.bugs;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
// this one is only uni-directional
public class EmploymentLink implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private int version;

    @JoinColumn(name = "person_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    // yes, final!
    private final Person person;

    @JoinColumn(name = "employer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private final Employer employer;

    private EmploymentLink() {
        this.person = null;
        this.employer = null;
    }

    public EmploymentLink(Person person, Employer employer) {
        this.person = person;
        this.employer = employer;
    }

    public Person getPerson() {
        return person;
    }
}
