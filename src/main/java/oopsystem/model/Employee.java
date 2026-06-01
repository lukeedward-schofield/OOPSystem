package oopsystem.model;

public class Employee {

    private int id;
    private String firstName;

    public Employee(){

    }

    public Employee(int id, String firstName){
        this.id = id;
        this.firstName = firstName;
    }

    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}

    public String getText(){return this.firstName;}
    public void setText(String text){this.firstName = firstName;}

}
