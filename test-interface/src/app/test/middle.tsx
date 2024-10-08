"use client";

import React, { useState, useEffect } from "react";
import { EyeIcon } from "@heroicons/react/24/outline";
import { MiddleProps } from "./interface";

export default function Middle({
  type,
  remainTime,
  index,
  total,
  onSubmit,
  isPause,
  isHiddenTime,
}: MiddleProps) {
  // Initial countdown time in seconds (36 minutes)
  const initialTime = remainTime;

  // State to manage the countdown timer
  const [timeLeft, setTimeLeft] = useState(initialTime);
  const [isTimeVisible, setIsTimeVisible] = useState(true);

  useEffect(() => {
    if (!remainTime) {
      return;
    }
    setTimeLeft(remainTime);
  }, [remainTime]);

  useEffect(() => {
    if (isPause) {
      return;
    }
    if (isHiddenTime) {
      return;
    }
    const timer = setInterval(() => {
      setTimeLeft((prevTime) => {
        if (prevTime > 0) {
          return prevTime - 1;
        } else {
          clearInterval(timer);
          onSubmit();
          return 0;
        }
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [isPause, onSubmit]); // eslint-disable-line

  // Format time into MM:SS
  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(minutes).padStart(2, "0")}:${String(secs).padStart(
      2,
      "0"
    )}`;
  };

  const handleHideTime = () => {
    setIsTimeVisible((prevState) => !prevState);
  };

  return (
    <div className="flex items-center justify-between p-4">
      <div className="flex items-center space-x-4">
        <span className="font-bold text-gray-800">{type}</span>
        {!isPause && (
          <>
            <span className="text-gray-600">|</span>
            <span className="text-gray-600">
              Question {index} of {total}
            </span>
          </>
        )}
      </div>
      {!isHiddenTime && (
        <>
          {" "}
          <div className="flex items-center space-x-2 text-gray-600">
            {isTimeVisible && <span>{formatTime(timeLeft)}</span>}
            <button
              onClick={handleHideTime}
              className="flex items-center space-x-2 bg-[#0D6B6E] text-white px-2 py-1 rounded-md hover:bg-[#025d5d] focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-[#027f80]"
            >
              <EyeIcon className="h-5 w-5" />
              <span>{isTimeVisible ? "Hide Time" : "Show Time"}</span>
            </button>
          </div>
        </>
      )}
    </div>
  );
}
