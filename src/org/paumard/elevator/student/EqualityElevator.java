package org.paumard.elevator.student;

import org.paumard.elevator.Building;
import org.paumard.elevator.Elevator;
import org.paumard.elevator.event.DIRECTION;
import org.paumard.elevator.model.Person;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EqualityElevator implements Elevator {
    private static final int ANGER_LIMIT_THRESHOLD = 180;
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
    		return this.destinations;
    	}
        
    	int numberOfPeopleWaiting = countWaitingPeople();
    	if (numberOfPeopleWaiting > 0) {
    		
    		List<Integer> destinations = destinationsToPickUpAngryPeople();
    		if (!destinations.isEmpty()) {
    			this.destinations = destinations;
    			return this.destinations;
    		}
    		
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
				this.destinations  = destinationFloorsForCurrentFloor;
				return this.destinations;
    		}
    	}
    	
    	return List.of(1);
    }

	private List<Integer> destinationsToPickUpAngryPeople() {
		
		for (int indexFloor = 0 ; indexFloor < Building.MAX_FLOOR ; indexFloor++) {
			List<Person> waitingList = this.peopleByFloor.get(indexFloor);
			if (!waitingList.isEmpty()) {
				Person mostPatientPerson = waitingList.get(0);
				LocalTime arrivalTime = mostPatientPerson.getArrivalTime();
				Duration waitingTime = Duration.between(arrivalTime, this.time); 
				long waitingTimeInSeconds = waitingTime.toSeconds();
				if (waitingTimeInSeconds >= ANGER_LIMIT_THRESHOLD) {
					List<Integer> result = List.of(indexFloor + 1, mostPatientPerson.getDestinationFloor());
					return new ArrayList<>(result);
				}
			}
		}
		return List.of();
	}

	private List<Integer> findDestinationFloors(List<Person> waitingListForCurrentFloor) {
		return waitingListForCurrentFloor.stream()
			.map(person -> person.getDestinationFloor())
			.distinct()
			.sorted()
			.collect(Collectors.toList());
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
		
		int indexOfCurrentFloor = floor-1;
		if(!peopleByFloor.get(indexOfCurrentFloor).isEmpty()) {
			List<Integer> intermediateDestinations = findDestinationFloors(peopleByFloor.get(floor -1));	
			List<Integer> additionnaFloors = weCanTakeThem(intermediateDestinations);
			destinations.addAll(destinations.size(), additionnaFloors);
		}
		
		this.currentFloor = floor;
	}

	@Override
	public void loadPeople(List<Person> people) {
		this.people.addAll(people);  
		int indexFloor = this.currentFloor -1;
		this.peopleByFloor.get(indexFloor).removeAll(people);
		
	}

	@Override
	public void unload(List<Person> person) {
		this.people.removeAll(people);
		
	}

	private List<Integer> weCanTakeThem(List<Integer> intermediateDestinations) {
		int indexOfCurrentFloor = this.currentFloor -1;  
		int signFromDirection;
		signFromDirection = signFromEnum();
		
		List<Integer> additionnalFloors = intermediateDestinations.stream()
				.filter(d -> signFromDirection * d > signFromDirection * (indexOfCurrentFloor) && !destinations.contains(d))
				.distinct()
				.sorted()
				.collect(Collectors.toList());
		return additionnalFloors;
	}

	private int signFromEnum() {
		int signFromDirection;
		if (this.direction == direction.UP)
			signFromDirection = 1;
		else
			signFromDirection = -1;
		return signFromDirection;
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
