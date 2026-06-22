import { useLocation, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";


function SuccessPage() {

  const location = useLocation();
  const navigate = useNavigate();


  const reservation = location.state?.reservation;
  const payment = location.state?.payment;



  const goToSeats = () => {
    navigate("/seats", {
      replace: true,
    });
  };



  return (

    <main>

      <Navbar />


      <section className="page-content">


        <h2>
          Booking Successful
        </h2>



        {!reservation || !payment ? (

          <p className="empty-state">
            No booking data available.
          </p>

        ) : (


          <div className="card details-card">


            <p>
              <strong>Reservation ID:</strong>
              {" "}
              {reservation.id}
            </p>


            <p>
              <strong>Seat Number:</strong>
              {" "}
              {reservation.seat_number}
            </p>


            <p>
              <strong>Payment ID:</strong>
              {" "}
              {payment.id}
            </p>


            <p>
              <strong>Payment Status:</strong>
              {" "}
              {payment.status}
            </p>


            <p>
              <strong>Amount:</strong>
              {" "}
              {payment.amount}
            </p>


            <p>
              <strong>Provider Reference:</strong>
              {" "}
              {payment.providerReference}
            </p>


          </div>

        )}



        <button
          className="btn"
          type="button"
          onClick={goToSeats}
        >
          Back to Seats
        </button>


      </section>

    </main>

  );
}


export default SuccessPage;