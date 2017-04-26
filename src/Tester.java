import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Tester {
	
	public static void main(String args[]){
		int count = Integer.parseInt(args[0]);
		List<Obj> list = new ArrayList<Obj>();
		
		try {
			FileReader read = null;
			BufferedReader br = null;
			String line;
			
			FileWriter write = new FileWriter("validate.txt");
			BufferedWriter bw = new BufferedWriter(write);
			
			for(int i=1; i<=count; i++){
				read = new FileReader("log-"+i+".txt");
				br = new BufferedReader(read);
				
				while((line = br.readLine()) != null){
					bw.write(line);
				}
				br.close();
				read.close();
			}
			
			write.close();
			bw.close();

			
			//logic to sort/count
			read = new FileReader("validate.txt");
			br = new BufferedReader(read);
			
			while((line = br.readLine()) != null){
				String split[] = line.split("\\s+");
				Obj node = new Obj();
				node.setNodeId(Integer.parseInt(split[0]));
				node.setType(split[1]);
				node.setTimeStamp(Integer.parseInt(split[2]));
				list.add(node);
			}
			
			Collections.sort(list, new Comparator<Obj>() {
				@Override
				public int compare(Obj arg0, Obj arg1) {
					// TODO Auto-generated method stub
					if(arg0.getTimeStamp() < arg1.getTimeStamp()){
						return -1;
					}else if(arg0.getTimeStamp() > arg1.getTimeStamp()){
						return +1;
					}
					return 0;
				}
			});
			
			//Computing
			int safeCount[] = null;
			for(int j=1; j<=list.size(); j+=2){
				if(list.size()%2 == 0){
					if(list.get(j).getNodeId() == list.get(j-1).getNodeId() && list.get(j).getType() == "RELEASE" && list.get(j-1).getType() == "START" && list.get(j).getTimeStamp() > list.get(j-1).getTimeStamp()){
						safeCount[list.get(j).getNodeId()]++;
					}
				}
			}
			
			write= new FileWriter("Results.txt");
			bw = new BufferedWriter(write);
			for(int i=1; i<=list.size();i++){
				bw.write("Total Critical Section Access count for node "+ i + " is: " + safeCount[i]);
			}
			
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

class Obj{
	String type;
	int nodeId;
	int timeStamp;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getNodeId() {
		return nodeId;
	}
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	public int getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(int timeStamp) {
		this.timeStamp = timeStamp;
	}
		
}
