package org.paumard.elevator.student;

import org.paumard.elevator.Building;
import org.paumard.elevator.Elevator;
import org.paumard.elevator.event.DIRECTION;
import org.paumard.elevator.model.Person;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class EqualityElevator implements Elevator {
	private static final int ANGER_LIMIT_THRESHOLD = 600;
	private DIRECTION direction = DIRECTION.UP;
	private int currentFloor = 1;
	private List<List<Person>> peopleByFloor = List.of();
	private List<Person> people = new ArrayList<>();
	private final int capacity;
	private LocalTime time;
	private List<Integer> destinations = new ArrayList<>();

	public EqualityElevator(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public void startsAtFloor(LocalTime time, int initialFloor) {
		this.time = time;
	}

	@Override
	public void peopleWaiting(List<List<Person>> peopleByFloor) {
		this.peopleByFloor = peopleByFloor;
	}

	@Override
	public List<Integer> chooseNextFloors() {
		if (!this.destinations.isEmpty()) {
			
			if(destinations.get(0) > currentFloor)
				direction = direction.UP;
			else
				direction = direction.DOWN;

			return this.destinations;
		}

		int numberOfPeopleWaiting = countWaitingPeople();
		if (numberOfPeopleWaiting > 0) {
			List<Integer> nonEmptyFloors = findNonEmptyFloor();
			int nonEmptyFloor = nonEmptyFloors.get(0);
			if (nonEmptyFloor != this.currentFloor) {
				return List.of(nonEmptyFloor);
			} else {
				int indexOfCurrentFloor = this.currentFloor - 1;
				List<Person> waitingListForCurrentFloor =
						this.peopleByFloor.get(indexOfCurrentFloor);

				List<Integer> destinationFloorsForCurrentFloor =
						findDestinationFloors(waitingListForCurrentFloor);
				destinations.addAll(destinationFloorsForCurrentFloor);

				destinations.sort(Comparator.naturalOrder());
				return destinations;
			}
		}
		return List.of(1);
	}

	private List<Integer> findDestinationFloors(List<Person> waitingListForCurrentFloor) {
		return waitingListForCurrentFloor.stream()
				.map(person -> person.getDestinationFloor())
				.distinct()
				.collect(Collectors.toList());
	}

	private int getPersonsFloor(Person person) {

		List<Person> list = peopleByFloor.stream()
				.filter(p -> p.contains(person))
				.findAny()
				.get();

		return peopleByFloor.indexOf(list);
	}

	private List<Integer> findNonEmptyFloor() {
		for (int indexFloor = 0 ; indexFloor < Building.MAX_FLOOR ; indexFloor++) {
			if (!peopleByFloor.get(indexFloor).isEmpty()) {
				return List.of(indexFloor + 1);
			}
		}
		return List.of(-1);
	}

	private int countWaitingPeople() {
		return peopleByFloor.stream()
				.mapToInt(list -> list.size())
				.sum();
	}

	@Override
	public void arriveAtFloor(int floor) {
		if (!this.destinations.isEmpty()) {
			this.destinations.remove(0);
		}
		
		System.out.println(this.time  +  "    " + destinations + "\n");
		
		this.currentFloor = floor;
	}

	int index;
	private List<Integer> WeCanTakeThemOnTheWay(){
		
		commutation = 1;
		List<Integer> intermediateFloors = new ArrayList<>();

		for (int indexFloor = currentFloor-2; indexFloor > 0 ; indexFloor --) {
			index = indexFloor;
			
			List<Person> waitingList = this.peopleByFloor.get(indexFloor);
			if (!waitingList.isEmpty()) {
				List<Person> sameDirection = waitingList.stream()
						.filter(p -> p.getDestinationFloor() < index+1)
						.collect(Collectors.toList());
				if (!sameDirection.isEmpty()) {
					intermediateFloors.add(indexFloor+1);
				}
			}
		}
		
		for (Integer integer : intermediateFloors) {
			destinations.addAll(findDestinationFloors
					(peopleByFloor.get(integer-1)).stream()
						  						  .filter(i -> i < integer)
						  						  .collect(Collectors.toList()));

		}

		return intermediateFloors;
	}

	int signFromDirection;
	private void weCanTakeThemOnStop(int floor) {
		if(destinations.size() == 0 && this.direction == direction.UP)
			signFromDirection = -1;
		else
			signFromDirection = 1;

		List<Integer> additionnalFloors = findDestinationFloors( peopleByFloor.get(floor)).stream()
				.filter(d -> signFromDirection * d > signFromDirection * (floor)
						&& !destinations.contains(d))
				.distinct()
				.collect(Collectors.toList());

		destinations.addAll(additionnalFloors);
		destinations.sort(Comparator.naturalOrder());

	}


	@Override
	public void loadPeople(List<Person> people) {
		this.people.addAll(people);  
		int indexFloor = this.currentFloor -1;
		this.peopleByFloor.get(indexFloor).removeAll(people);

	}

	int commutation=4;
	@Override
	public void unload(List<Person> person) {
		if (direction == direction.UP)
		weCanTakeThemOnStop(currentFloor-1);

		List<Integer> weCanTakeThemOnTheWay = new ArrayList<>();
		
		if(currentFloor == 10) {
			
			if (commutation > 0) {
				commutation --;			
				weCanTakeThemOnTheWay = List.of(1);
			}

			else {
				weCanTakeThemOnTheWay = WeCanTakeThemOnTheWay();
				commutation = 4;
			}
				
		destinations.addAll(weCanTakeThemOnTheWay);

		destinations = destinations.stream()
				.distinct()
				.collect(Collectors.toList());
		
		
			destinations.sort(Comparator.reverseOrder());
		}
		this.people.removeAll(people);
	}


	@Override
	public void newPersonWaitingAtFloor(int floor, Person person) {
		int indexFloor = floor - 1;
		this.peopleByFloor.get(indexFloor).add(person);
	}

	@Override
	public void lastPersonArrived() {
	}

	@Override
	public void timeIs(LocalTime time) {
		this.time = time;
	}

	@Override
	public void standByAtFloor(int currentFloor) {
	}
}