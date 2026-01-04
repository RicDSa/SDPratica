package ds.assignment.tom;

public class LamportClock {

    private int time = 0;

    public LamportClock(int time){
        this.time = time;
    }


    public int getTime() {
        return time;
    }


    public void setTime(int time) {
        this.time = time;
    }


    public void increment(){
        this.time++;
    }

}