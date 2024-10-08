"use client";

import { Bars3Icon, XMarkIcon } from "@heroicons/react/24/outline";
import { Disclosure } from "@headlessui/react";
import { useState } from "react";

const initialNavigation = [
  { name: "Dashboard", href: "/", current: true },
  { name: "Test", href: "test", current: false },
  { name: "Review", href: "review", current: false },
  { name: "Glossary", href: "#", current: false },
];

function classNames(...classes: (string | undefined)[]): string {
  return classes.filter(Boolean).join(" ");
}

export default function Header() {
  const [navigation, setNavigation] = useState(initialNavigation);

  const handleNavigationClick = (name: string) => {
    console.log('Navigation clicked:', name); // Debugging output
    setNavigation((prevNavigation) =>
      prevNavigation.map((item) =>
        item.name === name
          ? { ...item, current: true }
          : { ...item, current: false }
      )
    );
  };

  return (
    <Disclosure as="nav" className="bg-gray-800">
      {({ open }) => (
        <>
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="flex h-16 items-center justify-between">
              <div className="flex items-center">
                <div className="hidden md:block">
                  <div className="ml-10 flex items-baseline space-x-4">
                    {navigation.map((item) => (
                      <a
                        key={item.name}
                        href={item.href}
                        aria-current={item.current ? "page" : undefined}
                        className={classNames(
                          item.current
                            ? "bg-gray-900 text-white"
                            : "text-gray-300 hover:bg-gray-700 hover:text-white",
                          "rounded-md px-3 py-2 text-sm font-medium"
                        )}
                        onClick={() => handleNavigationClick(item.name)}
                      >
                        {item.name}
                      </a>
                    ))}
                  </div>
                </div>
              </div>
              <div className="-mr-2 flex md:hidden">
                {/* Mobile menu button */}
                <button
                  type="button"
                  className="inline-flex items-center justify-center rounded-md bg-gray-800 p-2 text-gray-400 hover:bg-gray-700 hover:text-white focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-gray-800"
                  aria-label="Toggle mobile menu"
                >
                  <span className="sr-only">Open main menu</span>
                  <Bars3Icon
                    className={`h-6 w-6 ${open ? "hidden" : "block"}`}
                  />
                  <XMarkIcon
                    className={`h-6 w-6 ${open ? "block" : "hidden"}`}
                  />
                </button>
              </div>
            </div>
          </div>

          {/* Conditionally render mobile menu */}
          {open && (
            <div className="md:hidden">
              <div className="space-y-1 px-2 pb-3 pt-2 sm:px-3">
                {navigation.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    aria-current={item.current ? "page" : undefined}
                    className={classNames(
                      item.current
                        ? "bg-gray-900 text-white"
                        : "text-gray-300 hover:bg-gray-700 hover:text-white",
                      "block rounded-md px-3 py-2 text-base font-medium"
                    )}
                    onClick={() => handleNavigationClick(item.name)}
                  >
                    {item.name}
                  </a>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </Disclosure>
  );
}