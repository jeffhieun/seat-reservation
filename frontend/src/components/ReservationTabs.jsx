import "./ReservationTabs.css";

const TABS = [
  "PENDING_PAYMENT",
  "CONFIRMED",
  "EXPIRED",
];

function ReservationTabs({
  activeTab,
  onTabChange,
  pendingCount,
  confirmedCount,
  expiredCount,
}) {
  const countMap = {
    PENDING_PAYMENT: pendingCount,
    CONFIRMED: confirmedCount,
    EXPIRED: expiredCount,
  };

  return (
    <div className="reservation-tabs" role="tablist" aria-label="Reservation status tabs">
      {TABS.map((tab) => (
        <button
          key={tab}
          type="button"
          role="tab"
          aria-selected={activeTab === tab}
          className={`reservation-tab ${activeTab === tab ? "reservation-tab-active" : ""}`}
          onClick={() => onTabChange(tab)}
        >
          {tab} ({countMap[tab] || 0})
        </button>
      ))}
    </div>
  );
}

export default ReservationTabs;

