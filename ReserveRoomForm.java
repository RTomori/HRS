/*
 * Copyright(C) 2007-2013 National Institute of Informatics, All rights reserved.
 */
package app.reservation;

import java.util.Date;

import app.AppException;

/**
 * Form class for Reserve Room
 * 
 */
public class ReserveRoomForm {

	private ReserveRoomControl reserveRoomHandler = new ReserveRoomControl();

	private Date stayingDate;
	private String reservationNumber;

	private ReserveRoomControl getReserveRoomHandler() {
		return reserveRoomHandler;
	}

	public String submitReservation() throws AppException {
		ReserveRoomControl reserveRoomHandler = getReserveRoomHandler();
		return reserveRoomHandler.makeReservation(stayingDate);
	}

	/* for cancel feature */
	public void cancelReservation() throws AppException {
		ReserveRoomControl reserveRoomHandler = getReserveRoomHandler();
		reserveRoomHandler.cancelReservation(reservationNumber);
	}

	public void setReservationNumber(String reservationNumber) {
		this.reservationNumber = reservationNumber;
	}
	
	public Date getStayingDate() {
		return stayingDate;
	}

	public void setStayingDate(Date stayingDate) {
		this.stayingDate = stayingDate;
	}

}
