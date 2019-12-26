package ara.manet.algorithm.election;

public class Pair<K, V> {
	private Integer num;
	private Long id;
	
	public Pair(Integer num, Long id) {
		this.num = num;
		this.id = id;
	}
	
	public Integer getNum() {return num;}
	public Long getId() {return id;}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Pair)) return false;
		Pair pairo = (Pair) o;
		return this.num.equals(pairo.getNum()) &&
				this.id.equals(pairo.getId());
	}
	
}
