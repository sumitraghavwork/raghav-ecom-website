package com.raghavEcomm.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.raghavEcomm.exceptions.FeedbackException;
import com.raghavEcomm.exceptions.LoginException;
import com.raghavEcomm.exceptions.OrderException;
import com.raghavEcomm.exceptions.UserException;
import com.raghavEcomm.model.CurrentUserSession;
import com.raghavEcomm.model.Customer;
import com.raghavEcomm.model.Feedback;
import com.raghavEcomm.model.Order;
import com.raghavEcomm.repository.CurrentUserSessionRepo;
import com.raghavEcomm.repository.CustomerRepo;
import com.raghavEcomm.repository.FeedbackRepo;
import com.raghavEcomm.repository.OrderRepo;

@Service
public class FeedbackServiceImplementation implements FeedbackService {

	@Autowired
	private FeedbackRepo fDao;

	@Autowired
	private CustomerRepo uRepo;

	@Autowired
	private CustomerService userService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CurrentUserSessionRepo csdao;

	@Override
	public Feedback updateFeedBack(Feedback feedback, String customerKey, Integer feedbackId)
			throws FeedbackException, LoginException, UserException {

		CurrentUserSession loggedInUser = csdao.findByUuid(customerKey);

		if (loggedInUser == null) {
			throw new LoginException("Invalid Key Entered");
		}

		if (loggedInUser.getCustomer() == false) {
			throw new UserException("Unauthorized Access! Only Customer can make changes");
		}

		Optional<Customer> existingUser = uRepo.findById(loggedInUser.getUserId());

		if (existingUser.isPresent()) {

			Customer user = existingUser.get();

			Optional<Feedback> feedbackOptional = fDao.findById(feedback.getFeedbackId());

			if (feedbackOptional.isPresent()) {

				Feedback feedbackUser = feedbackOptional.get();

				if (feedbackUser.getCustomer().getUserLoginId() != user.getUserLoginId())
					throw new UserException("Invalid User key Entered");

				feedback.setCustomer(feedbackUser.getCustomer());
				feedback.setOrder(feedbackUser.getOrder());

				Feedback updatedFeedback = fDao.save(feedback);

				return updatedFeedback;
			} else
				throw new FeedbackException("Invalid Feedback details...!");

		} else {
			throw new UserException("User Not Found");
		}

	}

	@Override
	public Feedback viewFeedBack(Integer feedbackId) throws FeedbackException {

		return fDao.findById(feedbackId)
				.orElseThrow(() -> new FeedbackException("No feedback found with given feedbackId :" + feedbackId));
	}

	@Override
	public List<Feedback> viewAllFeedBack() throws FeedbackException {
		List<Feedback> feedbacks = fDao.findAll();
		if (feedbacks.size() == 0) {
			throw new FeedbackException("No feedback submitted...!");
		}
		return feedbacks;
	}

	@Override
	public Feedback deleteFeedBack(String key, Integer feedbackId)
			throws FeedbackException, LoginException, UserException {

		CurrentUserSession loggedInUser = csdao.findByUuid(key);

		if (loggedInUser == null) {
			throw new LoginException("Invalid Key Entered");
		}

		Customer user = userService.findByUserLoginId(loggedInUser.getUserId());

		Optional<Feedback> feedback = fDao.findById(feedbackId);
		if (feedback.isPresent()) {
			Feedback existingfeedback = feedback.get();

			Customer feedbakUser = existingfeedback.getCustomer();

			if (feedbakUser.getUserLoginId() != user.getUserLoginId())
				throw new UserException("Invalid User key Entered");

			existingfeedback.setCustomer(null);
			existingfeedback.setOrder(null);

			fDao.delete(existingfeedback);

			return existingfeedback;
		} else {
			throw new FeedbackException("No feedback found with given feedbackId :" + feedbackId);
		}

	}

	@Override
	public Feedback addFeedBack(Feedback feedback, String key, Integer orderId)
			throws FeedbackException, UserException, OrderException, LoginException {
		CurrentUserSession loggedInUser = csdao.findByUuid(key);

		if (loggedInUser == null) {
			throw new UserException("user not logged in");
		}

		Order order = orderService.viewOrder(orderId, key);

		if (order != null)
			throw new OrderException("No reservations found!");

		Customer user = userService.findByUserLoginId(loggedInUser.getUserId());
		feedback.setCustomer(user);
		feedback.setOrder(order);

		Feedback newFeedback = fDao.save(feedback);
		return newFeedback;

	}

}
