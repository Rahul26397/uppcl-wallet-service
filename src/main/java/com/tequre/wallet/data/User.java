package com.tequre.wallet.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.tequre.wallet.enums.Role;
import com.tequre.wallet.enums.UserStatus;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@JsonInclude(Include.NON_NULL)
@Document(collection = "user")
public class User extends BaseEntity {

    private String firstName;

    private String lastName;

    private String email;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private String mobile;
    
    private String empId;

    private Address address;

    private Address residenceAddress;

    @Indexed(unique = true)
    private String userName;

    @Transient
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String imageUrl;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getResidenceAddress() {
        return residenceAddress;
    }

    public void setResidenceAddress(Address residenceAddress) {
        this.residenceAddress = residenceAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public void setEmpId(String empId) {
		// TODO Auto-generated method stub
		this.empId = empId;
	}

    @Override
    public String toString() {
        return "User{" +
                "id='" + super.getId() + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", mobile='" + mobile + '\'' +
                ", address=" + address +
                ", residenceAddress=" + residenceAddress +
                ", userName='" + userName + '\'' +
                ", role=" + role +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
