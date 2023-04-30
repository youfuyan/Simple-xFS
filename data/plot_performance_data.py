import pandas as pd
import matplotlib.pyplot as plt

# Read the CSV file
data = pd.read_csv("performance_data.csv")

# Create a line plot for the download time and latency with other peers
fig, ax = plt.subplots()
ax.plot(data.index, data["Download Time (ms)"],
        label="Download Time (ms)", linestyle="--", linewidth=2, color="black")
ax.plot(data.index, data["Latency_1_2 (ms)"], label="Latency Peer 1-2 (ms)")
ax.plot(data.index, data["Latency_1_3 (ms)"], label="Latency Peer 1-3 (ms)")
ax.plot(data.index, data["Latency_1_4 (ms)"], label="Latency Peer 1-4 (ms)")
ax.plot(data.index, data["Latency_1_5 (ms)"], label="Latency Peer 1-5 (ms)")

# Add markers for successful downloads
successful_downloads = data[data["Download Success"] == True]
ax.scatter(successful_downloads.index,
           successful_downloads["Download Time (ms)"], marker="o", color="red", label="Successful Downloads")

# Set plot labels and legend
ax.set_xlabel("Iteration")
ax.set_ylabel("Time (ms)")
ax.set_title("Download Time vs. Latency with Peers")
ax.legend()

# Save the plot as a PNG file
plt.savefig("download_time_vs_latency.png")

# Display the plot
plt.show()
